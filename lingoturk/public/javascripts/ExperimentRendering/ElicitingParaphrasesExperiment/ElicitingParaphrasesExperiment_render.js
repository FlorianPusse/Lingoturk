(function () {
    var app = angular.module('ElicitingParaphrasesExperimentApp', []);

    app.controller('RenderController', ['$http', '$timeout', function ($http, $timeout) {
        var self = this;
        self.part = null;
        self.questions = [];
        self.index = -1;
        self.workerId = "";
        self.feedback = "";
        self.expId = "";

        self.submitting = false;
        this.submitResults = function () {
            if(self.submitting == true){
                return;
            }

            self.submitting = true;

            var answerList = [];

            for (var i = 0; i < self.questions.length; i++) {
                var question = self.questions[i];
                var questionId = question.id;
                var answer = question.answer;
                answerList.push({questionId: questionId, answer: answer});
            }

            var result = {
                experimentType : "ElicitingParaphrasesExperiment",
                workerId: self.workerId,
                partId: partId,
                answers: answerList
            };

            $http.post("/submitResults", result)
                .success(function () {
                    $("#feedbackSlide").show();
                })
                .error(function () {
                    setTimeout(function () {
                        self.submitting = false;
                        self.submitResults();
                    }, 2000);
                });
        };

        this.startExperiment = function(){
            $("#instructionsSlide").hide();
            self.index++;
        };

        self.submittingFeedback = false;
        this.submitFeedback = function(){
            if(self.submittingFeedback == true){
                return;
            }

            self.submittingFeedback = true;

            $http.post("/submitFeedback", {workerId: self.workerId, expId: self.expId, feedback: self.feedback})
                .success(function () {
                    document.getElementById("redirect").click();
                })
                .error(function () {
                    setTimeout(function () {
                        self.submittingFeedback = false;
                        self.submitFeedback();
                    }, 2000);
                });
        };

        this.next = function(){
            if (self.index + 1 < self.questions.length) {
                self.index++;
            } else {
                self.submitResults();
            }
        };

        $(document).ready(function () {
            self.expId = ($("#expId").length > 0) ? $("#expId").val() : null;
            var partId = $("#partId").val();
            if (partId != "") {
                $http.get("/returnPart?partId=" + partId).success(function (data) {
                    var json = data;
                    self.part = json;
                    self.questions = json.questions;
                });
            }
            $(document).on("keypress", ":input:not(textarea)", function(event) {
                if (event.keyCode == 13) {
                    event.preventDefault();
                }
            });
        });
    }]);
})();


