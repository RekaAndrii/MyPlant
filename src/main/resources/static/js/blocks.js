$(function() {

    initBlockEvent();
    console.log( "ready!" );
});
function initBlockEvent(){
    blocks = $(".block-item");
    blocks.click(function(){
        span = $(this).find("span");
        blockName = $(span).text();
        $.ajax({
            url: "/block/execute",
            data: {"name": blockName},
            success: function(result){

            }});
    });
}
