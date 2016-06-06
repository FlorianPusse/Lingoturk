(function () {
    var app = angular.module('LinkingApp', []);

    app.controller('RenderController', ['$http', '$timeout', function ($http, $timeout) {
        var self = this;
        //this.workerId = -1;
        this.assignmentId = -1;
        this.partId = -1;
        this.combinationList = null;
        this.isDone = false;

        this.currentQuestionNumber = -1;

        this.submit = function () {

        };

        this.nextQuestion = function () {
            if (++self.currentQuestionNumber < self.combinationList.length - 1) {
                var combination = self.combinationList[self.currentQuestionNumber];
                var url = document.location.origin + "/render?id=" + combination.lhs + "&id2=" + combination.rhs + "&Type=question&hitId=null&assignmentId=null&workerId=" + self.workerId;
                $("#iFrame").attr("src", url);
            } else {
                self.finish();
            }
        };

        this.finish = function(){
            $("#iFrame").hide();
            $("#finish").show();
            $("#instructions").hide();
        };

        this.load = function(){
            $("#workerSlide").hide();
            $("#instructions").show();

            var expId = $("#expId").val();
            if (expId != "") {
                $http.get("/getPart?expId=" + expId + "&workerId=" + self.workerId).success(function (combinationList) {
                    self.partId = combinationList.id;
                    self.combinationList = combinationList.questions;

                    if(self.combinationList.length == 0){
                        self.finish();
                    }else{
                        self.nextQuestion();
                    }
                });
            }
        };

        $(document).ready(function () {
            var expId = $("#expId").val();

            if (expId != "") {
                $(document).on("input", ".textInput", function () {
                    if ($(this).val() != "") {
                        $(this).next().removeAttr("disabled");
                    } else {
                        $(this).next().attr("disabled", "disabled");
                    }
                });

                $(window).on("message", function(e) {
                    var data = e.originalEvent.data;
                    if(data == "FINISHED"){
                        self.nextQuestion();
                    }else{
                        alert("Unexpected answer!\n" + data)
                    }
                });
            }
        })

    }]);
})();
