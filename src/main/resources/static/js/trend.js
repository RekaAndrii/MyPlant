$(function() {
    initWeekdayBarChart();
    initTopBlocksDonut();
    initDailyLineChart();
});

var CHART_GREEN  = '#648637';
var CHART_COLORS = ['#648637', '#a9a534', '#5b8db8', '#b86b5b', '#7b5bb8', '#5bb8a4', '#b8955b'];

// ── 1. Executions per weekday (bar) ──────────────────────────────────────────

function initWeekdayBarChart() {
    $.ajax({
        url: '/trend/countPerDay?time=month',
        success: function(result) {
            renderWeekdayBar(result);
        },
        error: function() {
            renderWeekdayBar(null);
        }
    });
}

function renderWeekdayBar(trend) {
    var weekDays = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];
    var labels   = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
    var dataByDay = trend && trend.data ? trend.data : {};

    var chartData = [];
    weekDays.forEach(function(day, i) {
        var dayMap = dataByDay[day] || {};
        var total = 0;
        Object.keys(dayMap).forEach(function(k) { total += dayMap[k]; });
        chartData.push({ day: labels[i], count: total });
    });

    var hasData = chartData.some(function(d) { return d.count > 0; });
    if (!hasData) {
        $('#weekday-bar-chart').addClass('chart-empty').text('No data yet');
        return;
    }

    Morris.Bar({
        element: 'weekday-bar-chart',
        data: chartData,
        xkey: 'day',
        ykeys: ['count'],
        labels: ['Executions'],
        barColors: [CHART_GREEN],
        gridTextColor: '#888',
        gridLineColor: '#444',
        resize: true,
        hideHover: false
    });
}

// ── 2. Top habits donut ───────────────────────────────────────────────────────

function initTopBlocksDonut() {
    $.ajax({
        url: '/trend/countPerDay?time=month',
        success: function(result) {
            renderTopBlocksDonut(result);
        },
        error: function() {
            renderTopBlocksDonut(null);
        }
    });
}

function renderTopBlocksDonut(trend) {
    var dataByDay = trend && trend.data ? trend.data : {};

    // Aggregate total count per block name across all days
    var totals = {};
    Object.keys(dataByDay).forEach(function(day) {
        var dayMap = dataByDay[day] || {};
        Object.keys(dayMap).forEach(function(blockName) {
            totals[blockName] = (totals[blockName] || 0) + dayMap[blockName];
        });
    });

    var entries = Object.keys(totals).map(function(k) {
        return { label: k, value: totals[k] };
    });
    entries.sort(function(a, b) { return b.value - a.value; });

    if (entries.length === 0) {
        $('#top-blocks-donut').addClass('chart-empty').text('No data yet');
        return;
    }

    // Cap at top 6; group the rest as "Other"
    var MAX = 6;
    var donutData;
    if (entries.length <= MAX) {
        donutData = entries;
    } else {
        var top = entries.slice(0, MAX);
        var otherTotal = 0;
        entries.slice(MAX).forEach(function(e) { otherTotal += e.value; });
        donutData = top.concat([{ label: 'Other', value: otherTotal }]);
    }

    Morris.Donut({
        element: 'top-blocks-donut',
        data: donutData,
        colors: CHART_COLORS,
        resize: true,
        labelColor: '#ccc',
        backgroundColor: '#2a2a2a'
    });
}

// ── 3. Daily activity line (last 30 days) ─────────────────────────────────────

function initDailyLineChart() {
    $.ajax({
        url: '/trend/countPerDate',
        success: function(result) {
            renderDailyLine(result);
        },
        error: function() {
            renderDailyLine(null);
        }
    });
}

function renderDailyLine(data) {
    if (!data || data.length === 0) {
        $('#daily-line-chart').addClass('chart-empty').text('No data yet');
        return;
    }

    var hasActivity = data.some(function(d) { return d.count > 0; });
    if (!hasActivity) {
        $('#daily-line-chart').addClass('chart-empty').text('No executions in the last 30 days');
        return;
    }

    // Shorten date labels to "Jul 1" style to avoid crowding
    var chartData = data.map(function(d) {
        var parts = d.date.split('-'); // yyyy-MM-dd
        var months = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'];
        var label = months[parseInt(parts[1], 10) - 1] + ' ' + parseInt(parts[2], 10);
        return { date: label, count: d.count };
    });

    Morris.Line({
        element: 'daily-line-chart',
        data: chartData,
        xkey: 'date',
        ykeys: ['count'],
        labels: ['Executions'],
        lineColors: [CHART_GREEN],
        pointFillColors: [CHART_GREEN],
        gridTextColor: '#888',
        gridLineColor: '#444',
        lineWidth: 2,
        pointSize: 3,
        resize: true,
        hideHover: false,
        smooth: false
    });
}
