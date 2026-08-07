$(function() {
    initPomodoroTimer();
});

var POMODORO_TIMER_KEY = "myplant.pomodoroTimer";
var POMODORO_BREAK_READY_KEY = "myplant.pomodoroBreakReady";
var FOCUS_DURATION_MS = 25 * 60 * 1000;
var BREAK_DURATION_MS = 5 * 60 * 1000;
var PAUSE_TIMEOUT_MS = 5 * 60 * 1000;
var FOCUS_DURATION_SECONDS = 25 * 60;
var pomodoroIntervalId = null;
var pomodoroTagOptions = {
    goalSteps: [],
    blockNames: []
};
var goalStepLabelById = {};
var pomodoroSessionsById = {};

function initPomodoroTimer() {
    $("#startFocusBtn").click(startFocus);
    $("#cancelFocusBtn").click(cancelFocus);
    $("#pauseFocusBtn").click(pauseFocus);
    $("#resumeFocusBtn").click(resumeFocus);
    $("#startBreakBtn").click(startBreak);
    $("#endBreakBtn").click(endBreak);
    $("#pomodoroCancelTagsBtn").click(closePomodoroTagModal);
    $("#pomodoroSaveTagsBtn").click(saveSessionTags);

    $("#pomodoroTagModal").on("click", function(e) {
        if ($(e.target).is("#pomodoroTagModal")) {
            closePomodoroTagModal();
        }
    });

    $(document).on("click", ".pomodoro-session-delete", function() {
        var sessionId = $(this).attr("data-session-id");
        deleteSession(sessionId);
    });

    $(document).on("click", ".pomodoro-session-tags", function() {
        var sessionId = $(this).attr("data-session-id");
        openPomodoroTagModal(sessionId);
    });

    loadTagOptions();
    restorePomodoroTimer();
    loadPomodoroHistory();
}

function startFocus() {
    var now = Date.now();
    clearBreakReady();
    savePomodoroState({
        phase: "focus",
        startedAt: now,
        endsAt: now + FOCUS_DURATION_MS,
        paused: false
    });
    renderPomodoroTimer();
    startPomodoroTicking();
}

function cancelFocus() {
    var state = getPomodoroState();
    if (!state || state.phase !== "focus") {
        return;
    }
    clearPomodoroState();
    stopPomodoroTicking();
    saveFocusSession(state.startedAt, Date.now(), true);
    renderPomodoroTimer();
}

function pauseFocus() {
    var state = getPomodoroState();
    if (!state || state.phase !== "focus" || state.paused) {
        return;
    }

    state.paused = true;
    state.pausedAt = Date.now();
    state.pauseAutoCancelAt = state.pausedAt + PAUSE_TIMEOUT_MS;
    state.remainingMs = Math.max(0, state.endsAt - state.pausedAt);
    savePomodoroState(state);
    stopPomodoroTicking();
    startPomodoroTicking();
    renderPomodoroTimer();
}

function resumeFocus() {
    var state = getPomodoroState();
    if (!state || state.phase !== "focus" || !state.paused) {
        return;
    }

    if (Date.now() >= state.pauseAutoCancelAt) {
        autoCancelPausedFocus(state);
        return;
    }

    var now = Date.now();
    state.paused = false;
    state.endsAt = now + Math.max(0, state.remainingMs);
    delete state.pausedAt;
    delete state.pauseAutoCancelAt;
    delete state.remainingMs;
    savePomodoroState(state);
    renderPomodoroTimer();
    startPomodoroTicking();
}

function startBreak() {
    if (!isBreakReady()) {
        return;
    }
    var breakStart = Date.now();
    clearBreakReady();
    savePomodoroState({
        phase: "break",
        startedAt: breakStart,
        endsAt: breakStart + BREAK_DURATION_MS,
        paused: false
    });
    renderPomodoroTimer();
    startPomodoroTicking();
}

function endBreak() {
    clearPomodoroState();
    clearBreakReady();
    stopPomodoroTicking();
    renderPomodoroTimer();
}

function restorePomodoroTimer() {
    var state = getPomodoroState();
    if (!state) {
        renderPomodoroTimer();
        return;
    }
    renderPomodoroTimer();
    startPomodoroTicking();
}

function startPomodoroTicking() {
    stopPomodoroTicking();
    pomodoroIntervalId = window.setInterval(renderPomodoroTimer, 1000);
}

function stopPomodoroTicking() {
    if (pomodoroIntervalId !== null) {
        window.clearInterval(pomodoroIntervalId);
        pomodoroIntervalId = null;
    }
}

function renderPomodoroTimer() {
    var state = getPomodoroState();
    if (!state) {
        showPomodoroIdle();
        return;
    }

    if (state.phase === "focus" && state.paused) {
        var pauseRemaining = state.pauseAutoCancelAt - Date.now();
        if (pauseRemaining <= 0) {
            autoCancelPausedFocus(state);
            return;
        }
        showPomodoroPaused(state, pauseRemaining);
        return;
    }

    var remaining = state.endsAt - Date.now();
    if (remaining <= 0) {
        if (state.phase === "focus") {
            completeFocus(state);
        } else {
            completeBreak();
        }
        return;
    }

    showPomodoroPhase(state.phase, remaining);
}

function completeFocus(state) {
    clearPomodoroState();
    stopPomodoroTicking();
    saveFocusSession(state.startedAt, state.endsAt, false);
    setBreakReady();
    showBreakReady();
}

function completeBreak() {
    clearPomodoroState();
    clearBreakReady();
    stopPomodoroTicking();
    showPomodoroIdle();
}

function showPomodoroIdle() {
    $("#pomodoroPhase").text("Ready to focus");
    $("#pomodoroClock").text("25:00");
    $("#pomodoroHint").text("Complete a 25-minute focus session. Break start is manual.");
    $("#startFocusBtn").show();
    $("#cancelFocusBtn").hide();
    $("#pauseFocusBtn").hide();
    $("#resumeFocusBtn").hide();
    $("#startBreakBtn").hide();
    $("#endBreakBtn").hide();
    resetProgress();

    if (isBreakReady()) {
        showBreakReady();
    }
}

function showBreakReady() {
    $("#pomodoroPhase").text("Focus complete");
    $("#pomodoroClock").text("00:00");
    $("#pomodoroHint").text("Start your 5-minute break when you are ready.");
    $("#startFocusBtn").hide();
    $("#cancelFocusBtn").hide();
    $("#pauseFocusBtn").hide();
    $("#resumeFocusBtn").hide();
    $("#startBreakBtn").show();
    $("#endBreakBtn").hide();
    setProgress("focus", 0);
}

function showPomodoroPhase(phase, remaining) {
    var remainingSeconds = Math.ceil(remaining / 1000);
    $("#pomodoroClock").text(formatPomodoroDuration(remainingSeconds));
    if (phase === "focus") {
        $("#pomodoroPhase").text("Focus session");
        $("#pomodoroHint").text("Stay with your task until the timer reaches zero.");
        $("#startFocusBtn").hide();
        $("#cancelFocusBtn").show();
        $("#pauseFocusBtn").show();
        $("#resumeFocusBtn").hide();
        $("#startBreakBtn").hide();
        $("#endBreakBtn").hide();
        setProgress("focus", remaining);
    } else {
        $("#pomodoroPhase").text("Break");
        $("#pomodoroHint").text("Take a five-minute break, or end it early when you are ready.");
        $("#startFocusBtn").hide();
        $("#cancelFocusBtn").hide();
        $("#pauseFocusBtn").hide();
        $("#resumeFocusBtn").hide();
        $("#startBreakBtn").hide();
        $("#endBreakBtn").show();
        setProgress("break", remaining);
    }
}

function showPomodoroPaused(state, pauseRemaining) {
    var remainingSeconds = Math.ceil(Math.max(0, state.remainingMs) / 1000);
    $("#pomodoroPhase").text("Focus paused");
    $("#pomodoroClock").text(formatPomodoroDuration(remainingSeconds));
    $("#pomodoroHint").text("Resume within " + formatPomodoroDuration(Math.ceil(pauseRemaining / 1000)) + " or it will cancel.");
    $("#startFocusBtn").hide();
    $("#cancelFocusBtn").show();
    $("#pauseFocusBtn").hide();
    $("#resumeFocusBtn").show();
    $("#startBreakBtn").hide();
    $("#endBreakBtn").hide();
    setProgress("focus", state.remainingMs);
}

function formatPomodoroDuration(seconds) {
    var minutes = Math.floor(seconds / 60);
    var secondsPart = seconds % 60;
    return minutes + ":" + (secondsPart < 10 ? "0" : "") + secondsPart;
}

function setProgress(phase, remainingMs) {
    var durationMs = phase === "break" ? BREAK_DURATION_MS : FOCUS_DURATION_MS;
    var percent = Math.round(((durationMs - Math.max(0, remainingMs)) / durationMs) * 100);
    if (percent < 0) {
        percent = 0;
    }
    if (percent > 100) {
        percent = 100;
    }

    $("#pomodoroProgress")
        .removeClass("focus break")
        .addClass(phase)
        .css("width", percent + "%");
}

function resetProgress() {
    $("#pomodoroProgress")
        .removeClass("focus break")
        .css("width", "0%");
}

function saveFocusSession(startedAt, endedAt, cancelled) {
    $.ajax({
        url: "/pomodoro/sessions",
        method: "POST",
        contentType: "application/json",
        data: JSON.stringify({
            startedAt: new Date(startedAt).toISOString(),
            endedAt: new Date(endedAt).toISOString(),
            cancelled: cancelled
        }),
        success: function(result) {
            if (result.hasError === false) {
                loadPomodoroHistory();
            }
        }
    });
}

function loadPomodoroHistory() {
    $.ajax({
        url: "/pomodoro/sessions",
        method: "GET",
        success: function(sessions) {
            pomodoroSessionsById = {};
            var todayList = $("#pomodoroHistoryTodayItems");
            var olderList = $("#pomodoroHistoryOlderItems");
            todayList.empty();
            olderList.empty();
            if (!sessions || sessions.length === 0) {
                $("#pomodoroHistoryEmpty").show();
                $("#pomodoroHistoryToday").hide();
                $("#pomodoroHistoryOlder").hide();
                return;
            }

            $("#pomodoroHistoryEmpty").hide();
            var hasToday = false;
            var hasOlder = false;
            $.each(sessions, function(index, session) {
                pomodoroSessionsById[session.id] = session;

                var endedAt = new Date(session.endedAt);
                var item = buildHistoryItem(session, endedAt);
                if (isTodayDate(endedAt)) {
                    hasToday = true;
                    todayList.append(item);
                } else {
                    hasOlder = true;
                    olderList.append(item);
                }
            });

            $("#pomodoroHistoryToday").toggle(hasToday);
            $("#pomodoroHistoryOlder").toggle(hasOlder);
        }
    });
}

function buildHistoryItem(session, endedAt) {
    var status = session.cancelled ? "Cancelled" : "Completed";
    var duration = formatPomodoroDuration(session.elapsedSeconds);
    var range = formatSessionRange(session, endedAt);
    var percentage = Math.round((session.elapsedSeconds / FOCUS_DURATION_SECONDS) * 100);

    var tagsWrap = $("<div>").addClass("pomodoro-history-tags");
    renderSessionTags(session, tagsWrap);

    var detailsText = session.cancelled
        ? duration + " (" + percentage + "%)"
        : duration;

    return $("<div>").addClass("pomodoro-history-item").append(
        $("<div>").addClass("pomodoro-history-main").append(
            $("<span>").addClass("pomodoro-history-status")
                .addClass(session.cancelled ? "cancelled" : "completed").text(status),
            $("<span>").addClass("pomodoro-history-duration").text(detailsText),
            $("<span>").addClass("pomodoro-history-date").text(range)
        ),
        tagsWrap,
        $("<div>").addClass("pomodoro-history-actions").append(
            $("<button>")
                .addClass("btn btn-xs btn-default pomodoro-session-tags")
                .attr("type", "button")
                .attr("data-session-id", session.id)
                .text("Tags"),
            $("<button>")
                .addClass("btn btn-xs btn-danger pomodoro-session-delete")
                .attr("type", "button")
                .attr("data-session-id", session.id)
                .text("Delete")
        )
    );
}

function renderSessionTags(session, target) {
    var hasTags = false;

    if (session.goalStepIds && session.goalStepIds.length > 0) {
        hasTags = true;
        $.each(session.goalStepIds, function(index, goalStepId) {
            var goalStepLabel = goalStepLabelById[goalStepId] || goalStepId;
            target.append($("<span>").addClass("pomodoro-tag-pill").text(goalStepLabel));
        });
    }

    if (session.blockNames && session.blockNames.length > 0) {
        hasTags = true;
        $.each(session.blockNames, function(index, blockName) {
            target.append($("<span>").addClass("pomodoro-tag-pill block").text(blockName));
        });
    }

    if (!hasTags) {
        target.append($("<span>").addClass("pomodoro-empty-tag").text("No tags"));
    }
}

function formatSessionRange(session, endedAt) {
    var startedAt = new Date(session.startedAt);
    return startedAt.toLocaleString() + " - " + endedAt.toLocaleTimeString();
}

function isTodayDate(date) {
    var now = new Date();
    return date.getFullYear() === now.getFullYear()
        && date.getMonth() === now.getMonth()
        && date.getDate() === now.getDate();
}

function deleteSession(sessionId) {
    if (!sessionId) {
        return;
    }
    if (!window.confirm("Delete this session?")) {
        return;
    }

    $.ajax({
        url: "/pomodoro/sessions/" + encodeURIComponent(sessionId),
        method: "DELETE",
        success: function(result) {
            if (result.hasError === false) {
                loadPomodoroHistory();
            }
        }
    });
}

function loadTagOptions() {
    $.ajax({
        url: "/pomodoro/tags/options",
        method: "GET",
        success: function(options) {
            pomodoroTagOptions = options || { goalSteps: [], blockNames: [] };
            goalStepLabelById = {};
            $.each(pomodoroTagOptions.goalSteps || [], function(index, option) {
                goalStepLabelById[option.id] = option.label;
            });
            renderTagOptions();
            loadPomodoroHistory();
        }
    });
}

function renderTagOptions() {
    var goalStepsContainer = $("#pomodoroTagGoalSteps");
    var blocksContainer = $("#pomodoroTagBlocks");
    goalStepsContainer.empty();
    blocksContainer.empty();

    if (!pomodoroTagOptions.goalSteps || pomodoroTagOptions.goalSteps.length === 0) {
        goalStepsContainer.append($("<span>").addClass("text-muted").css("font-size", "12px").text("No goal steps available."));
    } else {
        $.each(pomodoroTagOptions.goalSteps, function(index, goalStep) {
            goalStepsContainer.append(
                $("<label>").addClass("step-block-pill").append(
                    $("<input>")
                        .attr("type", "checkbox")
                        .attr("value", goalStep.id),
                    $("<span>").text(goalStep.label)
                )
            );
        });
    }

    if (!pomodoroTagOptions.blockNames || pomodoroTagOptions.blockNames.length === 0) {
        blocksContainer.append($("<span>").addClass("text-muted").css("font-size", "12px").text("No blocks available."));
    } else {
        $.each(pomodoroTagOptions.blockNames, function(index, blockName) {
            blocksContainer.append(
                $("<label>").addClass("step-block-pill").append(
                    $("<input>")
                        .attr("type", "checkbox")
                        .attr("value", blockName),
                    $("<span>").text(blockName)
                )
            );
        });
    }
}

function openPomodoroTagModal(sessionId) {
    var session = pomodoroSessionsById[sessionId];
    if (!session) {
        return;
    }

    resetPomodoroTagModal();
    $("#pomodoroTagSessionId").val(sessionId);
    preCheckValues("#pomodoroTagGoalSteps", session.goalStepIds);
    preCheckValues("#pomodoroTagBlocks", session.blockNames);
    $("#pomodoroTagModal").show();
    setTimeout(function() {
        $("#pomodoroSaveTagsBtn").focus();
    }, 50);
}

function closePomodoroTagModal() {
    resetPomodoroTagModal();
    $("#pomodoroTagModal").hide();
}

function resetPomodoroTagModal() {
    $("#pomodoroTagSessionId").val("");
    $("#pomodoroTagGoalSteps input[type='checkbox']").prop("checked", false);
    $("#pomodoroTagBlocks input[type='checkbox']").prop("checked", false);
}

function preCheckValues(containerSelector, values) {
    if (!values) {
        return;
    }

    $(containerSelector + " input[type='checkbox']").each(function() {
        if (values.indexOf($(this).val()) !== -1) {
            $(this).prop("checked", true);
        }
    });
}

function collectCheckedValues(containerSelector) {
    var values = [];
    $(containerSelector + " input[type='checkbox']:checked").each(function() {
        values.push($(this).val());
    });
    return values;
}

function saveSessionTags() {
    var sessionId = $("#pomodoroTagSessionId").val();
    if (!sessionId) {
        return;
    }

    var goalStepIds = collectCheckedValues("#pomodoroTagGoalSteps");
    var blockNames = collectCheckedValues("#pomodoroTagBlocks");

    $.ajax({
        url: "/pomodoro/sessions/" + encodeURIComponent(sessionId) + "/tags",
        method: "PUT",
        contentType: "application/json",
        data: JSON.stringify({
            goalStepIds: goalStepIds,
            blockNames: blockNames
        }),
        success: function(result) {
            if (result.hasError === false) {
                closePomodoroTagModal();
                loadPomodoroHistory();
            }
        }
    });
}

function setBreakReady() {
    localStorage.setItem(POMODORO_BREAK_READY_KEY, "true");
}

function clearBreakReady() {
    localStorage.removeItem(POMODORO_BREAK_READY_KEY);
}

function isBreakReady() {
    return localStorage.getItem(POMODORO_BREAK_READY_KEY) === "true";
}

function autoCancelPausedFocus(state) {
    clearPomodoroState();
    clearBreakReady();
    stopPomodoroTicking();
    saveFocusSession(state.startedAt, Date.now(), true);
    showPomodoroIdle();
}

function getPomodoroState() {
    var raw = localStorage.getItem(POMODORO_TIMER_KEY);
    if (!raw) {
        return null;
    }
    try {
        var state = JSON.parse(raw);
        if (!state.phase || !state.startedAt || !state.endsAt) {
            clearPomodoroState();
            return null;
        }
        return state;
    } catch (e) {
        clearPomodoroState();
        return null;
    }
}

function savePomodoroState(state) {
    localStorage.setItem(POMODORO_TIMER_KEY, JSON.stringify(state));
}

function clearPomodoroState() {
    localStorage.removeItem(POMODORO_TIMER_KEY);
}
