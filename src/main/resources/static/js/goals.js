$(function () {
    initAddGoal();
    initEditGoal();
    initDeleteGoal();
    initGoalDoneToggle();
    initAddStep();
    initEditStep();
    initDeleteStep();
    initStepDoneToggle();
    initStepMove();
});

// ── Helpers ──────────────────────────────────────────────────────────────────

function closeModal(modalId) {
    $('#' + modalId).hide();
}

function openModal(modalId) {
    $('#' + modalId).show();
}

function collectCheckedPills(containerId) {
    var result = [];
    $('#' + containerId + ' input[type="checkbox"]:checked').each(function () {
        result.push($(this).val());
    });
    return result;
}

function uncheckAllPills(containerId) {
    $('#' + containerId + ' input[type="checkbox"]').prop('checked', false);
}

function preCheckPills(containerId, names) {
    uncheckAllPills(containerId);
    if (!names) { return; }
    var list = names.split(',');
    $('#' + containerId + ' input[type="checkbox"]').each(function () {
        if (list.indexOf($(this).val()) !== -1) {
            $(this).prop('checked', true);
        }
    });
}

// ── Add Goal ─────────────────────────────────────────────────────────────────

function resetAddGoalForm() {
    $('#newGoalName').val('');
}

function initAddGoal() {
    $('#addGoalBtn').on('click', function () {
        resetAddGoalForm();
        openModal('addGoalModal');
        setTimeout(function () { $('#newGoalName').focus(); }, 50);
    });

    $('#cancelGoalBtn').on('click', function () {
        resetAddGoalForm();
        closeModal('addGoalModal');
    });

    $('#addGoalModal').on('click', function (e) {
        if ($(e.target).is('#addGoalModal')) {
            resetAddGoalForm();
            closeModal('addGoalModal');
        }
    });

    $('#submitGoalBtn').on('click', function () {
        var name = $.trim($('#newGoalName').val());
        if (!name) { return; }
        $.ajax({
            url: '/goals/',
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify({ name: name }),
            success: function () {
                resetAddGoalForm();
                closeModal('addGoalModal');
                window.location.reload();
            }
        });
    });

    $('#newGoalName').on('keydown', function (e) {
        if (e.which === 13) { $('#submitGoalBtn').click(); }
    });
}

// ── Edit Goal ─────────────────────────────────────────────────────────────────

function resetEditGoalForm() {
    $('#editGoalId').val('');
    $('#editGoalName').val('');
}

function initEditGoal() {
    $(document).on('click', '.goal-edit-btn', function (e) {
        e.stopPropagation();
        var $card = $(this).closest('.goal-card');
        var goalId = $card.attr('data-goal-id');
        var goalName = $card.find('.goal-name').text();
        $('#editGoalId').val(goalId);
        $('#editGoalName').val(goalName);
        openModal('editGoalModal');
        setTimeout(function () { $('#editGoalName').focus(); }, 50);
    });

    $('#cancelEditGoalBtn').on('click', function () {
        resetEditGoalForm();
        closeModal('editGoalModal');
    });

    $('#editGoalModal').on('click', function (e) {
        if ($(e.target).is('#editGoalModal')) {
            resetEditGoalForm();
            closeModal('editGoalModal');
        }
    });

    $('#saveGoalBtn').on('click', function () {
        var id = $('#editGoalId').val();
        var name = $.trim($('#editGoalName').val());
        if (!name || !id) { return; }
        $.ajax({
            url: '/goals/' + encodeURIComponent(id),
            type: 'PUT',
            contentType: 'application/json',
            data: JSON.stringify({ name: name }),
            success: function () {
                resetEditGoalForm();
                closeModal('editGoalModal');
                window.location.reload();
            }
        });
    });

    $('#editGoalName').on('keydown', function (e) {
        if (e.which === 13) { $('#saveGoalBtn').click(); }
    });
}

// ── Delete Goal ───────────────────────────────────────────────────────────────

function initDeleteGoal() {
    $(document).on('click', '.goal-delete-btn', function (e) {
        e.stopPropagation();
        var $card = $(this).closest('.goal-card');
        $card.find('.goal-delete-confirm').show();
    });

    $(document).on('click', '.confirm-goal-delete-no', function (e) {
        e.stopPropagation();
        $(this).closest('.goal-delete-confirm').hide();
    });

    $(document).on('click', '.confirm-goal-delete-yes', function (e) {
        e.stopPropagation();
        var $card = $(this).closest('.goal-card');
        var goalId = $card.attr('data-goal-id');
        $.ajax({
            url: '/goals/' + encodeURIComponent(goalId),
            type: 'DELETE',
            success: function () {
                $card.fadeOut(300, function () { $(this).remove(); });
            }
        });
    });
}

// ── Goal Done Toggle ─────────────────────────────────────────────────────────

function initGoalDoneToggle() {
    $(document).on('change', '.goal-done-check', function () {
        var $card = $(this).closest('.goal-card');
        var goalId = $card.attr('data-goal-id');
        var done = $(this).is(':checked');
        $.ajax({
            url: '/goals/' + encodeURIComponent(goalId) + '/done',
            type: 'PUT',
            contentType: 'application/json',
            data: JSON.stringify({ done: done }),
            success: function () {
                if (done) {
                    $card.addClass('goal-done');
                } else {
                    $card.removeClass('goal-done');
                }
            }
        });
    });
}

// ── Add Step ──────────────────────────────────────────────────────────────────

function resetAddStepForm() {
    $('#addStepGoalId').val('');
    $('#newStepName').val('');
    uncheckAllPills('addStepBlocks');
}

function initAddStep() {
    $(document).on('click', '.add-step-btn', function (e) {
        e.stopPropagation();
        var goalId = $(this).attr('data-goal-id');
        resetAddStepForm();
        $('#addStepGoalId').val(goalId);
        openModal('addStepModal');
        setTimeout(function () { $('#newStepName').focus(); }, 50);
    });

    $('#cancelStepBtn').on('click', function () {
        resetAddStepForm();
        closeModal('addStepModal');
    });

    $('#addStepModal').on('click', function (e) {
        if ($(e.target).is('#addStepModal')) {
            resetAddStepForm();
            closeModal('addStepModal');
        }
    });

    $('#submitStepBtn').on('click', function () {
        var goalId = $('#addStepGoalId').val();
        var name = $.trim($('#newStepName').val());
        var linkedBlockNames = collectCheckedPills('addStepBlocks');
        if (!name || !goalId) { return; }
        $.ajax({
            url: '/goals/' + encodeURIComponent(goalId) + '/steps',
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify({ name: name, linkedBlockNames: linkedBlockNames }),
            success: function () {
                resetAddStepForm();
                closeModal('addStepModal');
                window.location.reload();
            }
        });
    });

    $('#newStepName').on('keydown', function (e) {
        if (e.which === 13) { $('#submitStepBtn').click(); }
    });
}

// ── Edit Step ─────────────────────────────────────────────────────────────────

function resetEditStepForm() {
    $('#editStepId').val('');
    $('#editStepName').val('');
    uncheckAllPills('editStepBlocks');
}

function initEditStep() {
    $(document).on('click', '.step-edit-btn', function (e) {
        e.stopPropagation();
        var $step = $(this).closest('.step-item');
        var stepId = $step.attr('data-step-id');
        var stepName = $(this).attr('data-step-name');
        var linkedBlocks = $(this).attr('data-linked-blocks') || '';
        resetEditStepForm();
        $('#editStepId').val(stepId);
        $('#editStepName').val(stepName);
        preCheckPills('editStepBlocks', linkedBlocks);
        openModal('editStepModal');
        setTimeout(function () { $('#editStepName').focus(); }, 50);
    });

    $('#cancelEditStepBtn').on('click', function () {
        resetEditStepForm();
        closeModal('editStepModal');
    });

    $('#editStepModal').on('click', function (e) {
        if ($(e.target).is('#editStepModal')) {
            resetEditStepForm();
            closeModal('editStepModal');
        }
    });

    $('#saveStepBtn').on('click', function () {
        var stepId = $('#editStepId').val();
        var name = $.trim($('#editStepName').val());
        var linkedBlockNames = collectCheckedPills('editStepBlocks');
        if (!name || !stepId) { return; }
        $.ajax({
            url: '/goals/steps/' + encodeURIComponent(stepId),
            type: 'PUT',
            contentType: 'application/json',
            data: JSON.stringify({ name: name, linkedBlockNames: linkedBlockNames }),
            success: function () {
                resetEditStepForm();
                closeModal('editStepModal');
                window.location.reload();
            }
        });
    });

    $('#editStepName').on('keydown', function (e) {
        if (e.which === 13) { $('#saveStepBtn').click(); }
    });
}

// ── Delete Step ───────────────────────────────────────────────────────────────

function initDeleteStep() {
    $(document).on('click', '.step-delete-btn', function (e) {
        e.stopPropagation();
        var $step = $(this).closest('.step-item');
        var stepId = $step.attr('data-step-id');
        if (!confirm('Delete this step?')) { return; }
        $.ajax({
            url: '/goals/steps/' + encodeURIComponent(stepId),
            type: 'DELETE',
            success: function () {
                $step.fadeOut(200, function () {
                    $(this).remove();
                });
            }
        });
    });
}

// ── Step Done Toggle ─────────────────────────────────────────────────────────

function initStepDoneToggle() {
    $(document).on('change', '.step-done-check', function () {
        var $step = $(this).closest('.step-item');
        var stepId = $step.attr('data-step-id');
        var done = $(this).is(':checked');
        $.ajax({
            url: '/goals/steps/' + encodeURIComponent(stepId) + '/done',
            type: 'PUT',
            contentType: 'application/json',
            data: JSON.stringify({ done: done }),
            success: function () {
                if (done) {
                    $step.addClass('step-done');
                } else {
                    $step.removeClass('step-done');
                }
            }
        });
    });
}

// ── Step Move ─────────────────────────────────────────────────────────────────

function initStepMove() {
    $(document).on('click', '.step-move-up', function (e) {
        e.stopPropagation();
        var $btn = $(this);
        if ($btn.is('[disabled]') || $btn.prop('disabled')) { return; }
        var stepId = $(this).closest('.step-item').attr('data-step-id');
        $.ajax({
            url: '/goals/steps/' + encodeURIComponent(stepId) + '/move',
            type: 'PUT',
            contentType: 'application/json',
            data: JSON.stringify({ direction: 'up' }),
            success: function () {
                window.location.reload();
            }
        });
    });

    $(document).on('click', '.step-move-down', function (e) {
        e.stopPropagation();
        var $btn = $(this);
        if ($btn.is('[disabled]') || $btn.prop('disabled')) { return; }
        var stepId = $(this).closest('.step-item').attr('data-step-id');
        $.ajax({
            url: '/goals/steps/' + encodeURIComponent(stepId) + '/move',
            type: 'PUT',
            contentType: 'application/json',
            data: JSON.stringify({ direction: 'down' }),
            success: function () {
                window.location.reload();
            }
        });
    });
}
