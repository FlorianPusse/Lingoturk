(function () {
    var app = angular.module('ElicitingParaphrasesExperimentApp', []);

    app.controller('RenderController', ['$http', '$timeout', function ($http, $timeout) {
        var self = this;
        self.part = null;
        self.questions = [];
        self.index = -1;
        self.workerId = "";

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
                    window.location.href = "https://prolificacademic.co.uk/submissions/5732eb0d961cde000d7a2857/complete?cc=FPEQXRKR";
                })
                .error(function () {
                    setTimeout(function () {
                        self.submitting = false;
                        self.submitResults()
                    }, 2000);
                });
        };

        this.startExperiment = function(){
            $("#instructionsSlide").hide();
            self.index++;
        };

        this.next = function(){
            if (self.index + 1 < self.questions.length) {
                self.index++;
            } else {
                self.submitResults();
            }
        };

        $(document).ready(function () {
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


