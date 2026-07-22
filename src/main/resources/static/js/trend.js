$(function() {

    initBlocksPerDayChart();
    console.log( "ready!" );
});

function initBlocksPerDayChart(){
  $.ajax({
    url: "/trend/countPerDay?time=month",
    success: function(result){
      renderWeeklyProgressChart(result);
    },
    error: function(){
      renderWeeklyProgressChart(null);
    }
  });
}

function renderWeeklyProgressChart(trend){
  var normalized = normalizeTrend(trend);

  Morris.Bar({
    element: 'weekly-progress-chart',
    data: normalized.data,
    xkey: 'day',
    ykeys: normalized.ykeys,
    labels: normalized.labels,
    stacked: true,
    resize: true
  });
}

function normalizeTrend(trend){
  var yValues = trend && trend.yValues && trend.yValues.length ? trend.yValues : ["No data"];
  var dataByDay = trend && trend.data ? trend.data : {};
  var weekDays = ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"];
  var chartData = [];

  weekDays.forEach(function(dayName){
    var dayData = $.extend({}, dataByDay[dayName] || {});
    dayData.day = dayName;

    yValues.forEach(function(yKey){
      if (dayData[yKey] == null) {
        dayData[yKey] = 0;
      }
    });

    chartData.push(dayData);
  });

  return {
    data: chartData,
    ykeys: yValues,
    labels: yValues
  };
}