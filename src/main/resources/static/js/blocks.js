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
    console.log( "ready!" );
});

function initBlockEvent() {
    var blocks = $(".block-item");
    blocks.click(function() {
        var span = $(this).find("span");
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

    $("#addBlockBtn").click(function() {
        var blockName = $("#newBlockName").val().trim();
        if (!blockName) {
            return;
        }

        var isChallenge = $("#isChallengeCheck").is(":checked");
        var payload = {name: blockName};

        if (isChallenge) {
            var target = parseInt($("#targetExecutions").val(), 10);
            if (!target || target < 1) {
                alert("Please enter a valid target execution count.");
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
                    $("#newBlockName").val("");
                    $("#isChallengeCheck").prop("checked", false);
                    $("#challengeOptions").hide();
                    $("#targetExecutions").val("");
                    window.location.reload();
                }
            }
        });
    });
}
