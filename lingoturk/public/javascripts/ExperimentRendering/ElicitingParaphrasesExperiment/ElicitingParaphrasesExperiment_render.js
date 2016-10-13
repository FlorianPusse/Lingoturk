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
                partId: self.partId,
                answers: answerList
            };

            $http.post("/submitResults", result)
                .success(function () {
                    self.index++;
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
            var partId = $("#partId").val();
            if (partId === undefined || partId == "") {
                $http.get("/getPart?expId=" + self.expId + "&workerId=" + self.workerId).success(function (data) {
                    var json = data;
                    self.part = json;
                    self.partId = json.id;
                    self.questions = json.questions;

                    shuffleArray(self.part.questions);
                });
            }

            $("#instructionsSlide").hide();
            self.index++;
        };

        this.checkAnswer = function(question){
            var modifiedTexts = [];
            for(var i = 0; i < self.questions.length; ++i){
                modifiedTexts.push(self.questions[i].text.toLowerCase().replace(/\s/g, '').replace(/[^a-z0-9]/g,''));
            }

            if(question.answer.split(" ").length < 5 || modifiedTexts.contains(question.answer.toLowerCase().replace(/\s/g, '').replace(/[^a-z0-9]/g,''))){
                return true;
            }else{
                return false;
            }
        };

        self.submittingFeedback = false;
        this.submitFeedback = function(){
            if(self.submittingFeedback == true){
                return;
            }

            self.submittingFeedback = true;

            $http.post("/submitFeedback", {workerId: self.workerId, expId: self.expId, feedback: self.feedback})
                .success(function () {
                    var url = "https://prolificacademic.co.uk/submissions/57e3c6b347180e0001f10e11/complete?cc=OYU1T7G4";
                    if(inIframe()){
                        window.top.location.href = url;
                    }else{
                        window.location = url;
                    }
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
            if (partId !== undefined && partId != "") {
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


