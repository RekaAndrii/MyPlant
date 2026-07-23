$(function() {

    // Redirect to login if any AJAX call is intercepted by Spring Security
    // (session expired: server returns 200 with the login page body)
    $(document).ajaxComplete(function (event, xhr) {
        if (xhr.responseURL && xhr.responseURL.indexOf('/login') !== -1) {
            window.location.href = '/login';
        }
    });

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

    $("#addBlockBtn").click(function(){
        var blockName = $("#newBlockName").val().trim();
        if(!blockName){
            return;
        }

        $.ajax({
            url: "/block/",
            method: "POST",
            contentType: "application/json",
            data: JSON.stringify({name: blockName}),
            success: function(result){
                if(result.hasError == false){
                    $("#newBlockName").val("");
                    window.location.reload();
                }
            }
        });
    });
}
