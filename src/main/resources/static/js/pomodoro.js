$(function() {
    initPomodoroTimer();
});

var POMODORO_TIMER_KEY = "myplant.pomodoroTimer";
var FOCUS_DURATION_MS = 25 * 60 * 1000;
var BREAK_DURATION_MS = 5 * 60 * 1000;
var pomodoroIntervalId = null;

function initPomodoroTimer() {
    $("#startFocusBtn").click(startFocus);
    $("#cancelFocusBtn").click(cancelFocus);
    $("#endBreakBtn").click(endBreak);
    restorePomodoroTimer();
    loadPomodoroHistory();
}

function startFocus() {
    var now = Date.now();
    savePomodoroState({
        phase: "focus",
        startedAt: now,
        endsAt: now + FOCUS_DURATION_MS
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

function endBreak() {
    clearPomodoroState();
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
    saveFocusSession(state.startedAt, state.endsAt, false);
    var breakStart = Date.now();
    savePomodoroState({
        phase: "break",
        startedAt: breakStart,
        endsAt: breakStart + BREAK_DURATION_MS
    });
    showPomodoroPhase("break", BREAK_DURATION_MS);
}

function completeBreak() {
    clearPomodoroState();
    stopPomodoroTicking();
    showPomodoroIdle();
}

function showPomodoroIdle() {
    $("#pomodoroPhase").text("Ready to focus");
    $("#pomodoroClock").text("25:00");
    $("#pomodoroHint").text("Complete a 25-minute focus session, then take a 5-minute break.");
    $("#startFocusBtn").show();
    $("#cancelFocusBtn").hide();
    $("#endBreakBtn").hide();
}

function showPomodoroPhase(phase, remaining) {
    var remainingSeconds = Math.ceil(remaining / 1000);
    $("#pomodoroClock").text(formatPomodoroDuration(remainingSeconds));
    if (phase === "focus") {
        $("#pomodoroPhase").text("Focus session");
        $("#pomodoroHint").text("Stay with your task until the timer reaches zero.");
        $("#startFocusBtn").hide();
        $("#cancelFocusBtn").show();
        $("#endBreakBtn").hide();
    } else {
        $("#pomodoroPhase").text("Break");
        $("#pomodoroHint").text("Take a five-minute break, or end it early when you are ready.");
        $("#startFocusBtn").hide();
        $("#cancelFocusBtn").hide();
        $("#endBreakBtn").show();
    }
}

function formatPomodoroDuration(seconds) {
    var minutes = Math.floor(seconds / 60);
    var secondsPart = seconds % 60;
    return minutes + ":" + (secondsPart < 10 ? "0" : "") + secondsPart;
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
            var history = $("#pomodoroHistory");
            history.empty();
            if (!sessions || sessions.length === 0) {
                $("#pomodoroHistoryEmpty").show();
                return;
            }

            $("#pomodoroHistoryEmpty").hide();
            $.each(sessions, function(index, session) {
                var status = session.cancelled ? "Cancelled" : "Completed";
                var duration = formatPomodoroDuration(session.elapsedSeconds);
                var endedAt = new Date(session.endedAt).toLocaleString();
                history.append(
                    $("<div>").addClass("pomodoro-history-item").append(
                        $("<span>").addClass("pomodoro-history-status")
                            .addClass(session.cancelled ? "cancelled" : "completed").text(status),
                        $("<span>").addClass("pomodoro-history-duration").text(duration),
                        $("<span>").addClass("pomodoro-history-date").text(endedAt)
                    )
                );
            });
        }
    });
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
