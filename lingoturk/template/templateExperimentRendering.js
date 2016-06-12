(function () {
    var app = angular.module('_TEMPLATE_ExperimentApp', ["Lingoturk"]);

    app.controller('RenderController', ['$http', '$timeout', '$scope', function ($http, $timeout, $scope) {
        var self = this;
        self.state = "";
        self.allStates = [];
        self.questions = [];
        self.part = null;
        self.slideIndex = 0;
        self.questionIndex = 0;
        self.questionId = null;
        self.partId = null;
        self.hitId = "";
        self.assignmentId = "";
        self.workerId = "";

        self.shuffleQuestions = true;

        this.resultsSubmitted = function(){
            bootbox.alert("Results successfully submitted. You might consider redirecting your participants now.");
        };

        this.resultSubmissionError = function(){
            bootbox.alert("Error! Could not submit results!");
        };

        self.failedTries = 0;
        this.submitResults = function (successCallback, errorCallback) {
            var results = {
                experimentType : "_TEMPLATE_Experiment",
                results : self.questions,
                assignmentId : self.assignmentId,
                hitId : self.hitId,
                workerId : self.workerId,
                partId : self.partId
            };


            $http.post("/submitResults", results)
                .success(function () {
                    successCallback();
                })
                .error(function () {
                    if(self.failedTries < 100){
                        ++self.failedTries;
                        setTimeout(function() { self.submitResults() }, 1000);
                    }else{
                        errorCallback();
                    }
                });
        };

        this.next = function(){
             if(self.state == "workerIdSlide"){
                if(self.questionId == null && self.partId == null){
                    self.load(function(){
                        self.state = self.allStates[++self.slideIndex];
                    });
                    return;
                }
            }

            if(self.slideIndex + 1 < self.allStates.length){
                self.state = self.allStates[++self.slideIndex];
            }else{
                self.submitResults(self.resultsSubmitted, self.resultSubmissionError);
            }
        };

        this.nextQuestion = function(){
            if(self.questionIndex + 1 < self.questions.length){
                ++self.questionIndex;
            }else{
                self.next();
            }
        };

        this.load = function(callback){
            if(self.questionId != null){
                $http.get("/getQuestion/" + self.questionId).success(function (data) {
                    self.questions = [data];
                });
            }else if(self.partId != null){
                $http.get("/returnPart?partId=" + self.partId).success(function (data) {
                    var json = data;
                    self.part = json;
                    self.questions = json.questions;

                    if(self.shuffleQuestions){
                        shuffleArray(self.part.questions);
                    }
                });
            }else{
                $http.get("/getPart?expId=" + self.expId + "&workerId=" + self.workerId).success(function (data) {
                    var json = data;
                    self.part = json;
                    self.questions = json.questions;

                    if(self.shuffleQuestions){
                        shuffleArray(self.part.questions);
                    }
                });
            }
            if(callback !== undefined){
                callback();
            }
        };

        $(document).ready(function () {
            self.questionId = ($("#questionId").length > 0) ? $("#questionId").val() : null;
            self.partId = ($("#partId").length > 0) ? $("#partId").val() : null;
            self.expId = ($("#expId").length > 0) ? $("#expId").val() : null;
            self.hitId = ($("#hitId").length > 0) ? $("#hitId").val() : "NOT AVAILABLE";
            self.assignmentId = ($("#assignmentId").length > 0) ? $("#assignmentId").val() : "NOT AVAILABLE";

            if(self.questionId != null || self.partId != null){
                self.load();
            }

            self.allStates = ["instructionsSlide","workerIdSlide","statisticsSlide","questionSlide"];
            $scope.$apply(self.state = self.allStates[0]);
            self.allStates.splice(0,1);
        });
    }]);
})();


