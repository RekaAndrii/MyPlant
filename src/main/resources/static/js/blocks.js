$(function() {

    // Redirect to login if any AJAX call is intercepted by Spring Security
    // (session expired: server returns 200 with the login page body)
    $(document).ajaxComplete(function (event, xhr) {
        if (xhr.responseURL && xhr.responseURL.indexOf('/login') !== -1) {
            window.location.href = '/login';
        }
    });

    initBlockEvent();
    initAddBlockEvent();
    initEditMode();
    console.log( "ready!" );
});

var editModeActive = false;

function initBlockEvent() {
    var blocks = $(".block-item").not("#addBlockBtn");
    blocks.click(function() {
        if (editModeActive) return;

        var span = $(this).children("span");
        var blockName = span.text();
        var block = $(this);

        block.removeClass("red").removeClass("yellow").removeClass("green");

        $.ajax({
            url: "/block/execute",
            data: {"name": blockName},
            success: function(result) {
                if (result.hasError == false) {
                    if (result.isChallenge) {
                        if (result.completed) {
                            // Challenge complete: flash "Done" then remove block
                            span.addClass("block-flash-text").text("Done");
                            block.addClass("green");
                            setTimeout(function() {
                                block.closest(".block-col").fadeOut(400, function() {
                                    $(this).remove();
                                });
                            }, 2000);
                        } else {
                            // Challenge in progress: flash remaining count then restore name
                            span.addClass("block-flash-text").text(result.remainingExecutions);
                            block.addClass("green");
                            setTimeout(function() {
                                span.removeClass("block-flash-text").text(blockName);
                            }, 2000);
                        }
                    } else {
                        block.addClass("green");
                    }
                }
            }
        });
    });
}

function initAddBlockEvent() {
    // Toggle challenge options on checkbox change
    $("#isChallengeCheck").change(function() {
        if ($(this).is(":checked")) {
            $("#challengeOptions").show();
        } else {
            $("#challengeOptions").hide();
            $("#targetExecutions").val("");
        }
    });

    // Open modal
    $("#addBlockBtn").click(function() {
        if (editModeActive) return;
        $("#addBlockModal").show();
        $("#newBlockName").focus();
    });

    // Close on overlay click outside the dialog
    $("#addBlockModal").click(function(e) {
        if ($(e.target).is("#addBlockModal")) {
            resetAddBlockForm();
        }
    });

    // Cancel: close form and reset
    $("#cancelBlockBtn").click(function() {
        resetAddBlockForm();
    });

    // Submit
    $("#submitBlockBtn").click(function() {
        var blockName = $("#newBlockName").val().trim();
        if (!blockName) {
            $("#newBlockName").focus();
            return;
        }

        var isChallenge = $("#isChallengeCheck").is(":checked");
        var payload = {name: blockName};

        if (isChallenge) {
            var target = parseInt($("#targetExecutions").val(), 10);
            if (!target || target < 1) {
                alert("Please enter a valid target execution count.");
                $("#targetExecutions").focus();
                return;
            }
            payload.isChallenge = true;
            payload.targetExecutions = target;
        }

        $.ajax({
            url: "/block/",
            method: "POST",
            contentType: "application/json",
            data: JSON.stringify(payload),
            success: function(result) {
                if (result.hasError == false) {
                    resetAddBlockForm();
                    window.location.reload();
                }
            }
        });
    });
}

function resetAddBlockForm() {
    $("#addBlockModal").hide();
    $("#newBlockName").val("");
    $("#isChallengeCheck").prop("checked", false);
    $("#challengeOptions").hide();
    $("#targetExecutions").val("");
}

function initEditMode() {
    // Toggle edit mode
    $("#editModeBtn").click(function() {
        editModeActive = !editModeActive;
        if (editModeActive) {
            $(this).text("Done").addClass("active");
            $(".block-edit-icons").show();
        } else {
            $(this).text("Edit").removeClass("active");
            $(".block-edit-icons").hide();
            $(".block-delete-confirm").hide();
        }
    });

    // Pencil: open edit modal pre-filled
    $(document).on("click", ".edit-pencil-btn", function(e) {
        e.stopPropagation();
        var tile = $(this).closest(".block-item");
        var blockName = tile.attr("data-block-name");
        $("#editBlockOriginalName").val(blockName);
        $("#editBlockName").val(blockName);
        // We don't have challenge data in the DOM, so just clear challenge fields
        $("#editIsChallengeCheck").prop("checked", false);
        $("#editChallengeOptions").hide();
        $("#editTargetExecutions").val("");
        $("#editBlockModal").show();
        $("#editBlockName").focus();
    });

    // Edit modal: toggle challenge options
    $("#editIsChallengeCheck").change(function() {
        if ($(this).is(":checked")) {
            $("#editChallengeOptions").show();
        } else {
            $("#editChallengeOptions").hide();
            $("#editTargetExecutions").val("");
        }
    });

    // Edit modal: close on overlay click
    $("#editBlockModal").click(function(e) {
        if ($(e.target).is("#editBlockModal")) {
            resetEditForm();
        }
    });

    // Edit modal: cancel
    $("#cancelEditBtn").click(function() {
        resetEditForm();
    });

    // Edit modal: save
    $("#saveEditBtn").click(function() {
        var originalName = $("#editBlockOriginalName").val();
        var newName = $("#editBlockName").val().trim();
        if (!newName) {
            $("#editBlockName").focus();
            return;
        }

        var isChallenge = $("#editIsChallengeCheck").is(":checked");
        var payload = {name: newName, isChallenge: isChallenge};

        if (isChallenge) {
            var target = parseInt($("#editTargetExecutions").val(), 10);
            if (!target || target < 1) {
                alert("Please enter a valid target execution count.");
                $("#editTargetExecutions").focus();
                return;
            }
            payload.targetExecutions = target;
        }

        $.ajax({
            url: "/block/" + encodeURIComponent(originalName),
            method: "PUT",
            contentType: "application/json",
            data: JSON.stringify(payload),
            success: function(result) {
                if (result.hasError == false) {
                    resetEditForm();
                    window.location.reload();
                }
            }
        });
    });

    // Trash: show delete confirmation on tile
    $(document).on("click", ".edit-trash-btn", function(e) {
        e.stopPropagation();
        var tile = $(this).closest(".block-item");
        tile.find(".block-delete-confirm").show();
    });

    // Delete confirm: No
    $(document).on("click", ".confirm-delete-no", function(e) {
        e.stopPropagation();
        $(this).closest(".block-delete-confirm").hide();
    });

    // Delete confirm: Yes
    $(document).on("click", ".confirm-delete-yes", function(e) {
        e.stopPropagation();
        var tile = $(this).closest(".block-item");
        var blockName = tile.attr("data-block-name");
        $.ajax({
            url: "/block/" + encodeURIComponent(blockName),
            method: "DELETE",
            success: function(result) {
                if (result.hasError == false) {
                    tile.closest(".block-col").fadeOut(400, function() {
                        $(this).remove();
                    });
                }
            }
        });
    });
}

function resetEditForm() {
    $("#editBlockModal").hide();
    $("#editBlockOriginalName").val("");
    $("#editBlockName").val("");
    $("#editIsChallengeCheck").prop("checked", false);
    $("#editChallengeOptions").hide();
    $("#editTargetExecutions").val("");
}
