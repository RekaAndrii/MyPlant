$(function() {

    initBlocksPerDayChart();
    console.log( "ready!" );
});

function initBlocksPerDayChart(){
    var trend;
     $.ajax({
        async: false,
        url: "/trend/countPetDay",
        success: function(result){
           trend = result;
        }
     });

initWeekDays(trend);

Morris.Bar({
  element: 'graph',
  data: [
    trend.data["MONDAY"],
    trend.data["TUESDAY"],
    trend.data["WEDNESDAY"],
    trend.data["THURSDAY"],
    trend.data["FRIDAY"],
    trend.data["SATURDAY"],
    trend.data["SUNDAY"]
  ],
  xkey: 'day',
  ykeys: trend.yValues,
  labels: trend.yValues,
  stacked: true
});



//Morris.Bar({
//  element: 'graph',
//  data: [
//    {x: '2011 Q1', y: 3, z: 2, a: 3},
//    {x: '2011 Q2', y: 2, z: null, a: 1},
//    {x: '2011 Q3', y: 0, z: 2, a: 4},
//    {x: '2011 Q4', y: 2, z: 4, a: 3}
//  ],
//  xkey: 'x',
//  ykeys: ['y', 'z', 'a'],
//  labels: ['Y', 'Z', 'A'],
//  stacked: true
//});

function initWeekDays(trend){
    trend.data["MONDAY"].day = "MONDAY";
    trend.data["TUESDAY"].day = "TUESDAY";
    trend.data["WEDNESDAY"].day = "WEDNESDAY";
    trend.data["THURSDAY"].day = "THURSDAY";
    trend.data["FRIDAY"].day = "FRIDAY";
    trend.data["SATURDAY"].day = "SATURDAY";
    trend.data["SUNDAY"].day = "SUNDAY";
}

}