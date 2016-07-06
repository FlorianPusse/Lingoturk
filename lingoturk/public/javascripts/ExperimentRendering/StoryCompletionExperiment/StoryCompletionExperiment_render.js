(function () {
    var app = angular.module('StoryCompletionExperimentApp', []);

    app.controller('RenderController', ['$http', '$timeout', function ($http, $timeout) {
        $(document).ready(function () {
            $(document).on("click",".nextButton",function(event){
                var parent = $(event.target).parents(".panel");
                parent.toggle();
                var next = parent.next(".panel");

                if(next.length != 0){
                    next.toggle();
                }
            });

            $("#workerIdButton").click(function(){
                $("#workerIdSlide").hide();

                var workerId = $("#workerId").val().trim();
                var expId = $("#expId").val();

                var url = "/getPart?workerId=" + encodeURIComponent(workerId) + "&expId=" + encodeURIComponent(expId);
                $.ajax(url,{
                    xhrFields: {
                        withCredentials: true
                    }
                }).done(function(response){
                    var parsedResponse = JSON.parse(response);
                    var overallLength = parsedResponse.questions.length;
                    for(var i = 0; i < overallLength;i++){
                        var story = parsedResponse.questions[i];
                        var newContent = '<div class="panel panel-primary hideSlide" style="width:90%;margin:auto"> <div class="panel-heading"> Story <strong style="float:right">' + (overallLength - i) + '/' + overallLength + '</strong> </div> <div class="panel-body">'
                        newContent += story.story;
                        newContent += '<hr/> <label>Please complete the story in maximally two sentences:</label> <textarea type="text" id="' + story.itemId + ',' + story.storyType + '" class="form-control textInput"></textarea> <button type="button" class="nextButton btn btn-default" disabled="disabled" style="float:right;margin-top:20px">Next</button> </div> </div>';
                        $("#workerIdSlide").after($(newContent));
                    }
                    $(".hideSlide").toggle();
                    $(".hideSlide").first().toggle();
                }).fail(function(){
                    $("#errorSlide").show();
                });

            });

            $(document).on("input",".textInput",function(){
                if($(this).val() != ""){
                    $(this).closest(".panel-body").find("button").removeAttr("disabled");
                }else{
                    $(this).closest(".panel-body").find("button").attr("disabled","disabled");
                }
            });

            $(document).on("keypress", ":input:not(textarea)", function(event) {
                if (event.keyCode == 13) {
                    event.preventDefault();
                }
            });

            var submitting = false;
            $("#submitButton").click(function(){
                if(!submitting){
                    submitting = true;

                    var wId = $("#workerId").val().trim();
                    var res = [];
                    $("textarea").each(function(){
                        res.push({
                            itemId : $(this).attr("id"),
                            result : $(this).val()
                        });
                    });

                    var d = JSON.stringify({ experimentType : "StoryCompletionExperiment", workerId : wId, results : res });

                    $.ajax({
                        type: "POST",
                        url : "/submitResults",
                        data : d,
                        contentType: "application/json"
                    }).done(function() {
                            window.location.href = "https://prolificacademic.co.uk/submissions/5732eb0d961cde000d7a2857/complete?cc=FPEQXRKR";
                    }).fail(function() {
                        alert("Error submitting your results. Please try again in some seconds.");
                        submitting = false;
                    })
                }
            })
        });
    }]);
})();