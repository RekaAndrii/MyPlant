$(function() {
    initBlockEvent();
    initAddBlockEvent();
    initEditMode();
    initDayFilter();
    console.log( "ready!" );
});

var editModeActive = false;

var DAY_FILTER_KEY = "myplant.dayFilter";

// JS Date.getDay(): Sun=0..Sat=6. Convert to ISO DayOfWeek: Mon=1..Sun=7.
function currentIsoWeekday() {
    var d = new Date().getDay();
    return d === 0 ? 7 : d;
}

// Collect checked day numbers (1..7) from a container into an array.
function collectScheduledDays(containerSelector) {
    var days = [];
    $(containerSelector).find("input[type=checkbox][data-day]:checked").each(function() {
        days.push(parseInt($(this).attr("data-day"), 10));
    });
    return days;
}

function initBlockEvent() {
    var blocks = $(".block-item").not("#addBlockBtn");
    blocks.click(function() {
        if (editModeActive) return;
        if ($(this).attr("data-disabled") === "true") return;

        var span = $(this).children("span");
        var blockName = span.text();
        var block = $(this);
        var wasGreen = block.hasClass("green");

        block.removeClass("red").removeClass("yellow").removeClass("green");

        $.ajax({
            url: "/block/execute",
            data: {"name": blockName},
            success: function(result) {
                if (result.hasError == false) {
                    if (!wasGreen) {
                        var current = parseInt($("#todayScore").text(), 10) || 0;
                        $("#todayScore").text(current + 1);
                    }
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

        var scheduledDays = collectScheduledDays("#addScheduledDays");
        if (scheduledDays.length > 0) {
            payload.scheduledDays = scheduledDays;
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
    $("#addScheduledDays").find("input[type=checkbox]").prop("checked", false);
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
        applyVisibility();
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
        // Pre-check scheduled days from the tile's data attribute
        $("#editScheduledDays").find("input[type=checkbox]").prop("checked", false);
        var scheduledDaysRaw = tile.attr("data-scheduled-days");
        if (scheduledDaysRaw) {
            var configuredDays = scheduledDaysRaw.split(",");
            $.each(configuredDays, function(i, day) {
                day = $.trim(day);
                if (day) {
                    $("#editScheduledDays").find("input[data-day='" + day + "']").prop("checked", true);
                }
            });
        }
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

        payload.scheduledDays = collectScheduledDays("#editScheduledDays");

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

    // Disable/enable: toggle block active status
    $(document).on("click", ".edit-disable-btn", function(e) {
        e.stopPropagation();
        var btn = $(this);
        var tile = btn.closest(".block-item");
        var blockName = tile.attr("data-block-name");
        var currentlyDisabled = tile.attr("data-disabled") === "true";
        var newDisabled = !currentlyDisabled;

        $.ajax({
            url: "/block/" + encodeURIComponent(blockName) + "/disabled",
            method: "PUT",
            data: {"disabled": newDisabled},
            success: function(result) {
                if (result.hasError == false) {
                    tile.attr("data-disabled", newDisabled);
                    tile.toggleClass("disabled-block", newDisabled);
                    if (newDisabled) {
                        btn.attr("title", "Enable").html("&#9654;");
                    } else {
                        btn.attr("title", "Disable").html("&#9208;");
                    }
                    applyVisibility();
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
    $("#editScheduledDays").find("input[type=checkbox]").prop("checked", false);
}

function initDayFilter() {
    // Restore persisted state (default: off)
    var active = localStorage.getItem(DAY_FILTER_KEY) === "on";
    setDayFilterState(active);

    $("#dayFilterBtn").click(function() {
        var nowActive = !($(this).hasClass("active"));
        localStorage.setItem(DAY_FILTER_KEY, nowActive ? "on" : "off");
        setDayFilterState(nowActive);
    });
}

function setDayFilterState(active) {
    var btn = $("#dayFilterBtn");
    if (active) {
        btn.addClass("active").text("Today only: On");
    } else {
        btn.removeClass("active").text("Today only: Off");
    }
    applyVisibility();
}

// Single source of truth for block-col visibility: combines the "today only"
// day filter with disabled-block hiding (disabled blocks are only shown while
// edit mode is active).
function applyVisibility() {
    var dayFilterActive = $("#dayFilterBtn").hasClass("active");
    var today = currentIsoWeekday();

    $(".block-item").not("#addBlockBtn").each(function() {
        var tile = $(this);
        var col = tile.closest(".block-col");

        // Disabled blocks are hidden unless edit mode is active.
        if (tile.attr("data-disabled") === "true" && !editModeActive) {
            col.hide();
            return;
        }

        if (!dayFilterActive) {
            col.show();
            return;
        }

        var raw = tile.attr("data-scheduled-days");
        // No days configured => always active (show every day)
        if (!raw || $.trim(raw) === "") {
            col.show();
            return;
        }

        var days = raw.split(",");
        var scheduledToday = false;
        $.each(days, function(i, day) {
            if (parseInt($.trim(day), 10) === today) {
                scheduledToday = true;
                return false; // break
            }
        });

        if (scheduledToday) {
            col.show();
        } else {
            col.hide();
        }
    });
}
