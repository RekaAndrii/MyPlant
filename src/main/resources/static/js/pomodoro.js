$(function() {
    initPomodoroTimer();
});

var POMODORO_TIMER_KEY = "myplant.pomodoroTimer";
var POMODORO_BREAK_READY_KEY = "myplant.pomodoroBreakReady";
var POMODORO_HISTORY_EXPANDED_KEY = "myplant.pomodoroHistoryExpanded";
var POMODORO_PRESET_KEY = "myplant.pomodoroPreset";
var POMODORO_MUTED_KEY = "myplant.pomodoroMuted";
var DEFAULT_TITLE = document.title;
var POMODORO_PRESETS = [
    { focusMinutes: 15, breakMinutes: 3 },
    { focusMinutes: 25, breakMinutes: 5 },
    { focusMinutes: 50, breakMinutes: 10 }
];
var activePreset = POMODORO_PRESETS[1];
var FOCUS_DURATION_MS = activePreset.focusMinutes * 60 * 1000;
var BREAK_DURATION_MS = activePreset.breakMinutes * 60 * 1000;
var PAUSE_TIMEOUT_MS = 5 * 60 * 1000;
var pomodoroIntervalId = null;
var pomodoroTagOptions = {
    goalSteps: [],
    blockNames: []
};
var goalStepLabelById = {};
var pomodoroSessionsById = {};
var preTagSelection = {
    goalStepIds: [],
    blockNames: []
};

function initPomodoroTimer() {
    $("#startFocusBtn").click(startFocus);
    $("#cancelFocusBtn").click(cancelFocus);
    $("#pauseFocusBtn").click(pauseFocus);
    $("#resumeFocusBtn").click(resumeFocus);
    $("#startBreakBtn").click(startBreak);
    $("#skipBreakBtn").click(skipBreak);
    $("#endBreakBtn").click(endBreak);
    $("#pomodoroCancelTagsBtn").click(closePomodoroTagModal);
    $("#pomodoroSaveTagsBtn").click(saveSessionTags);
    $("#pomodoroHistoryToggle").click(toggleOlderHistory);
    $("#pomodoroMuteBtn").click(toggleMuted);
    $("#pomodoroPreTagToggle").click(togglePreTagPanel);
    $(document).on("click", ".pomodoro-preset", function() {
        selectPreset($(this).data("focus"), $(this).data("break"));
    });
    $(document).on("change", "#pomodoroPreTagGoalSteps input, #pomodoroPreTagBlocks input", updatePreTagSelection);

    $("#pomodoroTagModal").on("click", function(e) {
        if ($(e.target).is("#pomodoroTagModal")) {
            closePomodoroTagModal();
        }
    });

    $(document).on("click", ".pomodoro-session-delete", function() {
        var sessionId = $(this).attr("data-session-id");
        showDeleteConfirm($(this).closest(".pomodoro-history-item"), sessionId);
    });

    $(document).on("click", ".pomodoro-session-tags", function() {
        var sessionId = $(this).attr("data-session-id");
        openPomodoroTagModal(sessionId);
    });

    loadStoredPreset();
    applyMutedState();
    loadTagOptions();
    restorePomodoroTimer();
    applyHistoryToggleState();
}

function startFocus() {
    requestNotificationPermission();
    var now = Date.now();
    clearBreakReady();
    savePomodoroState({
        phase: "focus",
        startedAt: now,
        endsAt: now + FOCUS_DURATION_MS,
        focusDurationMs: FOCUS_DURATION_MS,
        breakDurationMs: BREAK_DURATION_MS,
        plannedSeconds: activePreset.focusMinutes * 60,
        goalStepIds: preTagSelection.goalStepIds.slice(),
        blockNames: preTagSelection.blockNames.slice(),
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
    saveFocusSession(state, Date.now(), true);
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
    var readyState = getBreakReadyState();
    var breakDurationMs = (readyState && readyState.breakDurationMs) || BREAK_DURATION_MS;
    var breakStart = Date.now();
    clearBreakReady();
    savePomodoroState({
        phase: "break",
        startedAt: breakStart,
        endsAt: breakStart + breakDurationMs,
        breakDurationMs: breakDurationMs,
        paused: false
    });
    renderPomodoroTimer();
    startPomodoroTicking();
}

function skipBreak() {
    clearBreakReady();
    showPomodoroIdle();
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

    showPomodoroPhase(state, remaining);
}

function completeFocus(state) {
    clearPomodoroState();
    stopPomodoroTicking();
    saveFocusSession(state, state.endsAt, false);
    setBreakReady(state.breakDurationMs || BREAK_DURATION_MS);
    showBreakReady();
    notifyPhaseEnd("focus");
}

function completeBreak() {
    clearPomodoroState();
    clearBreakReady();
    stopPomodoroTicking();
    showPomodoroIdle();
    notifyPhaseEnd("break");
}

function showPomodoroIdle() {
    if (isBreakReady()) {
        showBreakReady();
        return;
    }

    setPhaseTint("idle");
    $("#pomodoroPhase").text("Ready to focus");
    $("#pomodoroClock").text(formatPomodoroDuration(activePreset.focusMinutes * 60));
    $("#pomodoroHint").text("Complete a " + activePreset.focusMinutes + "-minute focus session. Break start is manual.");
    setPomodoroButtons({ start: true });
    setPresetControlsEnabled(true);
    resetProgress();
    updateDocumentTitle(null, 0);
}

function showBreakReady() {
    setPhaseTint("break");
    $("#pomodoroPhase").text("Focus complete");
    $("#pomodoroClock").text("00:00");
    $("#pomodoroHint").text("Start your break when you are ready, or skip it to start another focus session.");
    setPomodoroButtons({ startBreak: true, skipBreak: true });
    setPresetControlsEnabled(true);
    setProgress("focus", 0);
    updateDocumentTitle("break-ready", 0);
}

function showPomodoroPhase(state, remaining) {
    var phase = state.phase;
    var remainingSeconds = Math.ceil(remaining / 1000);
    setPhaseTint(phase);
    $("#pomodoroClock").text(formatPomodoroDuration(remainingSeconds));
    updateDocumentTitle(phase, remainingSeconds);
    if (phase === "focus") {
        $("#pomodoroPhase").text("Focus session");
        $("#pomodoroHint").text("Stay with your task until the timer reaches zero.");
        setPomodoroButtons({ cancel: true, pause: true });
        setPresetControlsEnabled(false);
        setProgress("focus", remaining, state.focusDurationMs);
    } else {
        $("#pomodoroPhase").text("Break");
        $("#pomodoroHint").text("Take a break, or end it early when you are ready.");
        setPomodoroButtons({ endBreak: true });
        setPresetControlsEnabled(false);
        setProgress("break", remaining, state.breakDurationMs);
    }
}

function showPomodoroPaused(state, pauseRemaining) {
    var remainingSeconds = Math.ceil(Math.max(0, state.remainingMs) / 1000);
    setPhaseTint("paused");
    $("#pomodoroPhase").text("Focus paused");
    $("#pomodoroClock").text(formatPomodoroDuration(remainingSeconds));
    $("#pomodoroHint").text("Resume within " + formatPomodoroDuration(Math.ceil(pauseRemaining / 1000)) + " or it will cancel.");
    setPomodoroButtons({ cancel: true, resume: true });
    setPresetControlsEnabled(false);
    setProgress("paused", state.remainingMs, state.focusDurationMs || FOCUS_DURATION_MS);
    updateDocumentTitle("paused", remainingSeconds);
}

function setPhaseTint(phase) {
    $("#pomodoroPhase").removeClass("focus break paused idle").addClass(phase);
    $("#pomodoroClock").toggleClass("paused", phase === "paused");
}

function setPomodoroButtons(visible) {
    var buttons = {
        start: "#startFocusBtn",
        cancel: "#cancelFocusBtn",
        pause: "#pauseFocusBtn",
        resume: "#resumeFocusBtn",
        startBreak: "#startBreakBtn",
        skipBreak: "#skipBreakBtn",
        endBreak: "#endBreakBtn"
    };
    $.each(buttons, function(key, selector) {
        $(selector).toggle(!!visible[key]);
    });
}

/* ---- Duration presets ---- */

function loadStoredPreset() {
    var stored = localStorage.getItem(POMODORO_PRESET_KEY);
    var focusMinutes = stored ? parseInt(stored, 10) : activePreset.focusMinutes;
    var preset = findPreset(focusMinutes) || POMODORO_PRESETS[1];
    applyPreset(preset);
}

function findPreset(focusMinutes) {
    var found = null;
    $.each(POMODORO_PRESETS, function(index, preset) {
        if (preset.focusMinutes === focusMinutes) {
            found = preset;
        }
    });
    return found;
}

function selectPreset(focusMinutes, breakMinutes) {
    if (isTimerRunning()) {
        return;
    }
    var preset = findPreset(parseInt(focusMinutes, 10)) || { focusMinutes: parseInt(focusMinutes, 10), breakMinutes: parseInt(breakMinutes, 10) };
    applyPreset(preset);
    localStorage.setItem(POMODORO_PRESET_KEY, String(preset.focusMinutes));
}

function applyPreset(preset) {
    activePreset = preset;
    FOCUS_DURATION_MS = preset.focusMinutes * 60 * 1000;
    BREAK_DURATION_MS = preset.breakMinutes * 60 * 1000;
    $(".pomodoro-preset").each(function() {
        var isActive = parseInt($(this).data("focus"), 10) === preset.focusMinutes;
        $(this).toggleClass("active", isActive);
    });
    if (!getPomodoroState()) {
        showPomodoroIdle();
    }
}

function isTimerRunning() {
    var state = getPomodoroState();
    return !!state;
}

function setPresetControlsEnabled(enabled) {
    $(".pomodoro-preset").prop("disabled", !enabled);
    $("#pomodoroPreTagToggle").prop("disabled", !enabled);
    if (!enabled) {
        $("#pomodoroPreTagPanel").hide();
    }
}

/* ---- Alerts: sound, tab title, browser notification ---- */

function toggleMuted() {
    var muted = localStorage.getItem(POMODORO_MUTED_KEY) === "true";
    localStorage.setItem(POMODORO_MUTED_KEY, muted ? "false" : "true");
    applyMutedState();
}

function applyMutedState() {
    var muted = localStorage.getItem(POMODORO_MUTED_KEY) === "true";
    $("#pomodoroMuteBtn")
        .toggleClass("muted", muted)
        .html(muted ? "&#128263;" : "&#128266;");
}

function isMuted() {
    return localStorage.getItem(POMODORO_MUTED_KEY) === "true";
}

function notifyPhaseEnd(phase) {
    if (!isMuted()) {
        playPomodoroChime();
    }

    if (window.Notification && Notification.permission === "granted") {
        var message = phase === "focus"
            ? "Focus session complete. Time for a break."
            : "Break is over. Ready for another focus session.";
        try {
            new Notification("My Plant - Pomodoro Timer", { body: message });
        } catch (e) {
            // Notification construction can fail in some contexts; ignore.
        }
    }
}

function requestNotificationPermission() {
    if (window.Notification && Notification.permission === "default") {
        Notification.requestPermission();
    }
}

function playPomodoroChime() {
    try {
        var AudioContextClass = window.AudioContext || window.webkitAudioContext;
        if (!AudioContextClass) {
            return;
        }
        var ctx = new AudioContextClass();
        playTone(ctx, 660, 0, 0.15);
        playTone(ctx, 880, 0.16, 0.2);
    } catch (e) {
        // Audio can be blocked or unsupported; fail silently.
    }
}

function playTone(ctx, frequency, startOffset, duration) {
    var oscillator = ctx.createOscillator();
    var gain = ctx.createGain();
    oscillator.type = "sine";
    oscillator.frequency.value = frequency;
    gain.gain.value = 0.15;
    oscillator.connect(gain);
    gain.connect(ctx.destination);
    var startAt = ctx.currentTime + startOffset;
    oscillator.start(startAt);
    oscillator.stop(startAt + duration);
}

function updateDocumentTitle(phase, remainingSeconds) {
    if (!phase) {
        document.title = DEFAULT_TITLE;
        return;
    }
    if (phase === "break-ready") {
        document.title = "Break ready - My Plant";
        return;
    }
    var label = phase === "focus" ? "Focus" : (phase === "break" ? "Break" : "Paused");
    document.title = formatPomodoroDuration(remainingSeconds) + " - " + label + " - My Plant";
}

/* ---- Working-on pre-tag picker ---- */

function togglePreTagPanel() {
    $("#pomodoroPreTagPanel").toggle();
}

function updatePreTagSelection() {
    preTagSelection.goalStepIds = collectCheckedValues("#pomodoroPreTagGoalSteps");
    preTagSelection.blockNames = collectCheckedValues("#pomodoroPreTagBlocks");
    renderPreTagSummary();
}

function renderPreTagSummary() {
    var total = preTagSelection.goalStepIds.length + preTagSelection.blockNames.length;
    $("#pomodoroPreTagCount").text(total > 0 ? "(" + total + ")" : "");

    var summary = $("#pomodoroPreTagSummary").empty();
    $.each(preTagSelection.goalStepIds, function(index, goalStepId) {
        summary.append($("<span>").addClass("pomodoro-tag-pill").text(goalStepLabelById[goalStepId] || goalStepId));
    });
    $.each(preTagSelection.blockNames, function(index, blockName) {
        summary.append($("<span>").addClass("pomodoro-tag-pill block").text(blockName));
    });
}

function renderPreTagOptions() {
    renderPillOptions("#pomodoroPreTagGoalSteps", pomodoroTagOptions.goalSteps, "id", "label", "No goal steps available.");
    renderPillOptions("#pomodoroPreTagBlocks", pomodoroTagOptions.blockNames, null, null, "No blocks available.");
}

function renderPillOptions(containerSelector, items, valueKey, labelKey, emptyText) {
    var container = $(containerSelector);
    container.empty();
    if (!items || items.length === 0) {
        container.append($("<span>").addClass("text-muted").css("font-size", "12px").text(emptyText));
        return;
    }
    $.each(items, function(index, item) {
        var value = valueKey ? item[valueKey] : item;
        var label = labelKey ? item[labelKey] : item;
        container.append(
            $("<label>").addClass("step-block-pill").append(
                $("<input>").attr("type", "checkbox").attr("value", value),
                $("<span>").text(label)
            )
        );
    });
}

function formatPomodoroDuration(seconds) {
    var minutes = Math.floor(seconds / 60);
    var secondsPart = seconds % 60;
    return minutes + ":" + (secondsPart < 10 ? "0" : "") + secondsPart;
}

function setProgress(phase, remainingMs, durationOverrideMs) {
    var durationMs = durationOverrideMs || (phase === "break" ? BREAK_DURATION_MS : FOCUS_DURATION_MS);
    var percent = Math.round(((durationMs - Math.max(0, remainingMs)) / durationMs) * 100);
    if (percent < 0) {
        percent = 0;
    }
    if (percent > 100) {
        percent = 100;
    }

    $("#pomodoroProgress")
        .removeClass("focus break paused")
        .addClass(phase)
        .css("width", percent + "%");
}

function resetProgress() {
    $("#pomodoroProgress")
        .removeClass("focus break paused")
        .css("width", "0%");
}

function saveFocusSession(state, endedAt, cancelled) {
    $.ajax({
        url: "/pomodoro/sessions",
        method: "POST",
        contentType: "application/json",
        data: JSON.stringify({
            startedAt: new Date(state.startedAt).toISOString(),
            endedAt: new Date(endedAt).toISOString(),
            cancelled: cancelled,
            plannedSeconds: state.plannedSeconds || (activePreset.focusMinutes * 60),
            goalStepIds: state.goalStepIds || [],
            blockNames: state.blockNames || []
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
            $("#pomodoroTodaySummary").empty();

            if (!sessions || sessions.length === 0) {
                $("#pomodoroHistoryEmpty").show();
                $("#pomodoroHistoryToday").hide();
                $("#pomodoroHistoryToggle").hide();
                $("#pomodoroHistoryOlder").hide();
                return;
            }

            $.each(sessions, function(index, session) {
                pomodoroSessionsById[session.id] = session;
            });

            var grouped = groupSessionsByDay(sessions);

            $("#pomodoroHistoryEmpty").hide();

            if (grouped.today.length > 0) {
                $("#pomodoroHistoryToday").show();
                renderTodaySummary(grouped.today);
                $.each(grouped.today, function(index, session) {
                    todayList.append(buildHistoryItem(session, false));
                });
            } else {
                $("#pomodoroHistoryToday").hide();
            }

            if (grouped.olderDays.length > 0) {
                $("#pomodoroHistoryToggle").show();
                $.each(grouped.olderDays, function(index, day) {
                    var dayGroup = $("<div>").addClass("pomodoro-day-group").append(
                        $("<div>").addClass("pomodoro-day-label").text(day.label)
                    );
                    $.each(day.items, function(itemIndex, session) {
                        dayGroup.append(buildHistoryItem(session, true));
                    });
                    olderList.append(dayGroup);
                });
            } else {
                $("#pomodoroHistoryToggle").hide();
                $("#pomodoroHistoryOlder").hide();
            }

            applyHistoryToggleState();
        }
    });
}

function groupSessionsByDay(sessions) {
    var today = [];
    var olderByKey = {};
    var olderKeyOrder = [];

    $.each(sessions, function(index, session) {
        var endedAt = new Date(session.endedAt);
        if (isTodayDate(endedAt)) {
            today.push(session);
            return;
        }

        var dayKey = endedAt.getFullYear() + "-" + endedAt.getMonth() + "-" + endedAt.getDate();
        if (!olderByKey[dayKey]) {
            olderByKey[dayKey] = { label: formatDayLabel(endedAt), items: [] };
            olderKeyOrder.push(dayKey);
        }
        olderByKey[dayKey].items.push(session);
    });

    var olderDays = [];
    $.each(olderKeyOrder, function(index, key) {
        olderDays.push(olderByKey[key]);
    });

    return { today: today, olderDays: olderDays };
}

function formatDayLabel(date) {
    var now = new Date();
    var yesterday = new Date(now.getFullYear(), now.getMonth(), now.getDate() - 1);
    if (date.getFullYear() === yesterday.getFullYear()
        && date.getMonth() === yesterday.getMonth()
        && date.getDate() === yesterday.getDate()) {
        return "Yesterday";
    }

    var options = { weekday: "short", day: "numeric", month: "short" };
    if (date.getFullYear() !== now.getFullYear()) {
        options.year = "numeric";
    }
    return date.toLocaleDateString(undefined, options);
}

function renderTodaySummary(todaySessions) {
    var completed = 0;
    var cancelled = 0;
    var totalSeconds = 0;

    $.each(todaySessions, function(index, session) {
        if (session.cancelled) {
            cancelled++;
        } else {
            completed++;
            totalSeconds += session.elapsedSeconds;
        }
    });

    var container = $("#pomodoroTodaySummary");
    container.append($("<span>").addClass("pomodoro-summary-chip completed").text(completed + " completed"));
    if (cancelled > 0) {
        container.append($("<span>").addClass("pomodoro-summary-chip cancelled").text(cancelled + " cancelled"));
    }
    if (totalSeconds > 0) {
        container.append($("<span>").addClass("pomodoro-summary-chip time").text(formatTotalMinutes(totalSeconds) + " focused"));
    }
}

function formatTotalMinutes(totalSeconds) {
    var minutes = Math.round(totalSeconds / 60);
    var hours = Math.floor(minutes / 60);
    var remainingMinutes = minutes % 60;
    if (hours > 0) {
        return hours + "h " + remainingMinutes + "m";
    }
    return minutes + "m";
}

function toggleOlderHistory() {
    var expanded = $("#pomodoroHistoryOlder").is(":visible");
    setHistoryExpanded(!expanded);
}

function setHistoryExpanded(expanded) {
    localStorage.setItem(POMODORO_HISTORY_EXPANDED_KEY, expanded ? "true" : "false");
    applyHistoryToggleState();
}

function applyHistoryToggleState() {
    var expanded = localStorage.getItem(POMODORO_HISTORY_EXPANDED_KEY) === "true";
    var hasOlderSessions = $("#pomodoroHistoryToggle").is(":visible");
    $("#pomodoroHistoryOlder").toggle(hasOlderSessions && expanded);
    $("#pomodoroHistoryToggle").toggleClass("expanded", expanded);
    $("#pomodoroHistoryToggleLabel").text(expanded ? "Hide previous sessions" : "Show previous sessions");
}

function buildHistoryItem(session, isOlder) {
    var endedAt = new Date(session.endedAt);
    var status = session.cancelled ? "Cancelled" : "Completed";
    var duration = formatPomodoroDuration(session.elapsedSeconds);
    var range = formatSessionRange(session, endedAt);
    var plannedSeconds = session.plannedSeconds || 1500;
    var percentage = Math.round((session.elapsedSeconds / plannedSeconds) * 100);

    var tagsWrap = $("<div>").addClass("pomodoro-history-tags");
    renderSessionTags(session, tagsWrap);

    var detailsText = session.cancelled
        ? duration + " (" + percentage + "%)"
        : duration;

    var item = $("<div>").addClass("pomodoro-history-item").attr("data-session-id", session.id);
    if (isOlder) {
        item.addClass("older");
    }

    return item.append(
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

function showDeleteConfirm(item, sessionId) {
    if (!item || item.length === 0 || !sessionId) {
        return;
    }
    item.find(".pomodoro-delete-confirm").remove();

    var confirmBar = $("<div>").addClass("pomodoro-delete-confirm").append(
        $("<span>").text("Delete this session?"),
        $("<button>").addClass("btn btn-xs btn-danger").attr("type", "button").text("Yes").click(function(e) {
            e.stopPropagation();
            deleteSession(sessionId);
        }),
        $("<button>").addClass("btn btn-xs btn-default").attr("type", "button").text("No").click(function(e) {
            e.stopPropagation();
            confirmBar.remove();
        })
    );
    item.append(confirmBar);
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
            renderPreTagOptions();
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

function setBreakReady(breakDurationMs) {
    localStorage.setItem(POMODORO_BREAK_READY_KEY, JSON.stringify({ breakDurationMs: breakDurationMs }));
}

function clearBreakReady() {
    localStorage.removeItem(POMODORO_BREAK_READY_KEY);
}

function isBreakReady() {
    return localStorage.getItem(POMODORO_BREAK_READY_KEY) !== null;
}

function getBreakReadyState() {
    var raw = localStorage.getItem(POMODORO_BREAK_READY_KEY);
    if (!raw) {
        return null;
    }
    try {
        return JSON.parse(raw);
    } catch (e) {
        return null;
    }
}

function autoCancelPausedFocus(state) {
    clearPomodoroState();
    clearBreakReady();
    stopPomodoroTicking();
    saveFocusSession(state, Date.now(), true);
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
