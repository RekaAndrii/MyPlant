$(function() {

    initBlockEvent();
    console.log( "ready!" );
});
function initBlockEvent(){
    blocks = $(".block-item");
    blocks.click(function(){
        span = $(this).find("span");
        blockName = $(span).text();
        block = $(this);
        
        block.removeClass("red");
        block.removeClass("yellow");
        block.removeClass("green");
        
        $.ajax({
            url: "/block/execute",
            data: {"name": blockName},
            success: function(result){
                if(result.hasError == false){
                    block.addClass("green");
                }
            }});
    });
}
