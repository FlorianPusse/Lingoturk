(function () {
    var app = angular.module('VerbalWorkingMemoryExperimentApp', ["Lingoturk"]);

    app.controller('RenderController', ['$http', '$timeout', '$scope', function ($http, $timeout, $scope) {
        var self = this;
        self.state = "";
        self.allStates = [];
        self.questions = [];
        self.part = null;
        self.slideIndex = 0;
        self.questionIndex = 0;
        self.expId = null;
        self.questionId = null;
        self.partId = null;
        self.origin = null;
        self.hitId = "";
        self.assignmentId = "";
        self.workerId = "";
        self.subListMap = {};
        self.subListsIds = [];
        self.showMessage = "none";
        self.redirectUrl = null;

        self.shuffleQuestions = false;
        self.shuffleSublists = true;
        self.useGoodByeMessage = true;
        self.useStatistics = false;

        self.preIndex = 0;
        self.allPreItems = ["It was the gangsters that broke into the warehouse.",
        "It was the elephant that escaped from the zoo.",
            "It was the actor that donated a large sum of money.",
            "It was the professor that forgot the beamer.",
            "It was the man that clenched the pillow."];

        self.allPreItems2 = ["It was the gangsters that broke into the warehouse.",
            "It was the elephant that escaped from the zoo.",
            "It was the actor that donated a large sum of money.",
            "It was the professor that forgot the beamer.",
            "It was the man that clenched the pillow."];

        self.preDecisionTimes = [];
        self.measureStart = -1;

        self.divisions = [];
        self.divisionIndex = 0;


        self.decisionTime = null;
        self.rephrasingTime = null;

        self.statistics = [
            {name : "Age", type: "number", answer : undefined},
            {name : "Gender", type: "text", answer : ""},
            {name : "Nationality", type: "text", answer : ""},
            {name : "Mother's first language", type: "text", answer : ""},
            {name : "Father's first language", type: "text", answer : ""},
            {name : "Are you bilingual (grown up with more than one language)?", type: "boolean", answer : undefined},
            {name : "Please list the languages you speak at at the advance level.", type: "text", answer : "", optional : true}
        ];

        this.resultsSubmitted = function(){
            self.subListsIds.splice(0,1);
            if(self.subListsIds.length > 0 ){
                self.showMessage = "nextSubList";
            }else{
                self.processFinish();
            }
        };

        this.processFinish = function(){
            if(!self.useGoodByeMessage){
                self.finished();
            }else{
                self.showMessage = "goodBye";
            }
        };

        self.preMeasure2Init = function(){
            setTimeout(function(){$(".accetability-choice").removeAttr("disabled")},1000);
            self.decisionTimer = setTimeout(function(){ self.setPreDecision("timeout") }, self.decisionTime);
        };

        self.preMeasureReset = function(){
            setTimeout(function(){$(".accetability-choice").removeAttr("disabled")},1000);
            self.measureStart =  new Date().getTime();
        };

        this.finished = function(){
            if(self.origin == null || self.origin == "NOT AVAILABLE"){
                bootbox.alert("Results successfully submitted. You might consider redirecting your participants now.");
            }else if(self.origin == "MTURK"){
                $("#form").submit();
            }else if(self.origin == "PROLIFIC"){
				if(inIframe()){
                    window.top.location.href = self.redirectUrl;
                }else{
                    window.location = self.redirectUrl;
                }
            }
        };

        this.nextSublist = function(){
            self.questionIndex = 0;
            self.questions = self.subListMap[self.subListsIds[0]];
            self.processQuestions();
            self.showMessage = "none";
        };

        this.resultSubmissionError = function(){
            self.failedTries = 0;
            bootbox.alert("An error occurred while submitting your results. Please try again in a few seconds.");
        };

        this.handleError = function(){
            if(self.failedTries < 100){
                ++self.failedTries;
                setTimeout(function() { self.submitResults(self.resultsSubmitted, self.handleError) }, 1000);
            }else{
                self.resultSubmissionError();
            }
        };

        self.failedTries = 0;
        this.submitResults = function (successCallback, errorCallback) {
            var results = {
                experimentType : "VerbalWorkingMemoryExperiment",
                results : self.questions,
                expId : self.expId,
                origin : self.origin,
                statistics : self.statistics,
                assignmentId : self.assignmentId,
                hitId : self.hitId,
                workerId : self.workerId,
                partId : (self.partId == null ? -1 : self.partId)
            };


            $http.post("/submitResults", results)
                .success(successCallback)
                .error(errorCallback);
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
                self.submitResults(self.resultsSubmitted, self.handleError);
            }
        };

        this.nextQuestion = function(){
            if(self.state === "questionSlide"){
                if(self.decisionTimer !== null){
                    clearTimeout(self.decisionTimer);
                    clearTimeout(self.disabledTimer);
                    self.decisionTimer = null;
                    self.disabledTimer = null;
                }
                if(self.questionIndex + 1 < self.questions.length && self.questionIndex + 1 < self.divisions[self.divisionIndex]){
                    ++self.questionIndex;
                }else{
                    self.state = "questionSlide2";
                }
            }
            else if(self.state === "questionSlide2"){
                if(self.rephrasingTimer !== null){
                    clearTimeout(self.rephrasingTimer);
                    self.rephrasingTimer = null;
                }
                if(self.questionIndex + 1 < self.questions.length){
                    self.state = "questionSlide";
                    self.divisionIndex++;
                    self.questionIndex++;
                }else{
                    self.next();
                }
            }
        };

        this.showRephraseQuestion = function(index){
            if(self.divisionIndex === 0 && index < self.divisions[0]){
                return true;
            }
            if(self.divisionIndex > 0 && index >= self.divisions[self.divisionIndex-1] && index < self.divisions[self.divisionIndex]){
                return true;
            }
            return false;
        };

        self.getMean = function(arr){
            var sum = 0;
            for(var i = 0; i < arr.length; ++i){
                sum += arr[i];
            }
            return sum / arr.length;
        };

        self.getStdDev = function(arr){
            var mean = self.getMean(arr);
            var sum = 0;
            for(var i = 0; i < arr.length; ++i){
                sum += Math.pow(arr[i] - mean,2);
            }
            return Math.sqrt(sum / arr.length);
        };

        self.setPreDecision = function(decision){
            if(self.decisionTimer !== null){
                clearTimeout(self.decisionTimer);
                self.decisionTimer = null;
            }

            var timeTaken = Math.round(new Date().getTime() - self.measureStart);
            self.preDecisionTimes.push(timeTaken);
            if(self.preIndex + 1 < self.allPreItems.length){
                ++self.preIndex;
            }else{
                if(self.decisionTime === null){
                    var mean = self.getMean(self.preDecisionTimes);
                    var deviation = self.getStdDev(self.preDecisionTimes);
                    var multiplicator = 3;
                    var bias = 3000;
                    self.decisionTime = Math.round(mean + multiplicator*deviation);
                    self.rephrasingTime = self.decisionTime + bias;
                }
                self.preIndex = 0;
                self.next();
            }
        };

        self.decisionTimer = null;
        self.disabledTimer = null;
        this.startDecisionTimeout = function(){
            self.decisionTimer = setTimeout(function(){ self.decisionTimeout() }, self.decisionTime);
            self.disabledTimer = setTimeout(function(){$(".accetability-choice").removeAttr("disabled")},1500);
        };

        this.decisionTimeout = function(){
            var question = self.questions[self.questionIndex];
            $scope.$apply(self.setDecision(question,"timeout"));
        };

        this.resetTimers = function(){
            if(self.decisionTimer !== null){
                clearTimeout(self.decisionTimer);
                clearTimeout(self.disabledTimer);
                self.decisionTimer = null;
                self.disabledTimer = null;
            }
            if(self.rephrasingTimer !== null){
                clearTimeout(self.rephrasingTimer);
                self.rephrasingTimer = null;
            }
        };

        this.setDecision = function(question,decision){
            if(self.decisionTimer !== null){
                clearTimeout(self.decisionTimer);
                clearTimeout(self.disabledTimer);
                self.decisionTimer = null;
                self.disabledTimer = null;
            }
            question.answer = {};
            question.answer.decision = decision;
            self.nextQuestion();
        };

        self.rephrasingTimer = null;
        this.startRephrasingTimeout = function(){
            self.rephrasingTimer = setTimeout(function(){ self.rephrasingTimeout() }, self.rephrasingTime*(self.questions.length-1));
        };

        this.rephrasingTimeout = function(){
            if(self.rephrasingTimer !== null){
                clearTimeout(self.rephrasingTimer);
                self.rephrasingTimer = null;
            }
            for(var i = 0; i < self.questions.length; ++i){
                if(!self.questions[i].answer.hasOwnProperty("lastWord") || self.questions[i].answer.lastWord.trim() === ""){
                    self.questions[i].answer.lastWord = "timeout";
                }
            }
            $scope.$apply(self.nextQuestion());
        };

        this.activateNextInput = function(index){
            var next = index + 1;
            if ($("#input_" + next).length > 0){
                $("#input_" + next).removeAttr("disabled");
            }
        };

        this.allQuestionsAnswered = function(){
            for(var i = 0; i < self.divisions[self.divisionIndex]; ++i){
                if(!self.questions[i].hasOwnProperty("answer")|| !self.questions[i].answer.hasOwnProperty("lastWord") || self.questions[i].answer.lastWord.trim() === ""){
                    return false;
                }
            }
            return true;
        };

        this.allPreDone = function(){
            for(var i = 0; i < self.allPreItems2.length; ++i){
                if($("#input_" + i).length === 0 || $("#input_" + i).val().trim() === ""){
                    return false;
                }
            }
            return true;
        };


        this.questionLoadHandler = function(){
            for(var i = 0; i < self.questions.length; ++i){
                var q = self.questions[i];
                q.useInput = true;
            }
        };

        self.processQuestions = function(){
            var index = 0;
            while(index < self.questions.length){
                var distances = [2,3,4,5,6];
                while(distances.length > 0 && index < self.questions.length){
                    var e = randomElement(distances);
                    distances.splice(distances.indexOf(e),1);
                    index += e;
                    self.divisions.push(Math.min(index,self.questions.length));
                }
            }

        };

        this.load = function(callback){
            var subListMap = self.subListMap;

            if(self.questionId != null){
                $http.get("/getQuestion/" + self.questionId).success(function (data) {
                    self.questions = [data];
                    self.questionLoadHandler();

                    subListMap[self.questions[0].subList] = [self.questions[0]];
                    self.processQuestions();

                    if(callback !== undefined){
                        callback();
                    }
                });
            }else if(self.partId != null){
                $http.get("/returnPart?partId=" + self.partId).success(function (data) {
                    var json = data;
                    self.part = json;
                    self.questions = json.questions;
                    self.questionLoadHandler();

                    if(self.shuffleQuestions){
                        shuffleArray(self.part.questions);
                    }

                    for(var i = 0; i < self.questions.length; ++i){
                        var q = self.questions[i];
                        if (subListMap.hasOwnProperty(q.subList)){
                            subListMap[q.subList].push(q);
                        }else{
                            subListMap[q.subList] = [q];
                            self.subListsIds.push(q.subList);
                        }
                    }
                    if(self.shuffleSublists){
                        shuffleArray(self.subListsIds);
                    }
                    self.questions = self.subListMap[self.subListsIds[0]];
                    self.processQuestions();

                    if(callback !== undefined){
                        callback();
                    }
                });
            }else{
                $http.get("/getPart?expId=" + self.expId + "&workerId=" + self.workerId).success(function (data) {
                    var json = data;
                    self.part = json;
                    self.partId = json.id;
                    self.questions = json.questions;
                    self.questionLoadHandler();

                    if(self.shuffleQuestions){
                        shuffleArray(self.part.questions);
                    }

                    for(var i = 0; i < self.questions.length; ++i){
                        var q = self.questions[i];
                        if (subListMap.hasOwnProperty(q.subList)){
                            subListMap[q.subList].push(q);
                        }else{
                            subListMap[q.subList] = [q];
                            self.subListsIds.push(q.subList);
                        }
                    }
                    if(self.shuffleSublists){
                        shuffleArray(self.subListsIds);
                    }
                    self.questions = self.subListMap[self.subListsIds[0]];
                    self.processQuestions();

                    if(callback !== undefined){
                        callback();
                    }
                });
            }
        };

        $(document).ready(function () {
            self.questionId = ($("#questionId").length > 0) ? $("#questionId").val() : null;
            self.partId = ($("#partId").length > 0) ? $("#partId").val() : null;
            self.expId = ($("#expId").length > 0) ? $("#expId").val() : null;
            self.hitId = ($("#hitId").length > 0) ? $("#hitId").val() : "NOT AVAILABLE";
            self.workerId = ($("#workerId").length > 0) ? $("#workerId").val() : "";
            self.assignmentId = ($("#assignmentId").length > 0) ? $("#assignmentId").val() : "NOT AVAILABLE";
            self.origin = ($("#origin").length > 0) ? $("#origin").val() : "NOT AVAILABLE";
            self.redirectUrl = ($("#redirectUrl").length > 0) ? $("#redirectUrl").val() : null;

            if(self.questionId != null || self.partId != null){
                self.load();
            }

            self.allStates = ["instructionsSlide","workerIdSlide","statisticsSlide","preSlide", "intermediateSlide","preSlide2", "preSlide2_2", "intermediateSlide2", "questionSlide"];

            if(!self.useStatistics){
                var index = self.allStates.indexOf("statisticsSlide");
                self.allStates.splice(index,1);
            }

            if(self.workerId.trim() != ""){
                var index = self.allStates.indexOf("workerIdSlide");
                self.allStates.splice(index,1);
            }

            $scope.$apply(self.state = self.allStates[0]);

            $(document).on("keypress", ":input:not(textarea)", function(event) {
                if (event.keyCode == 13) {
                    event.preventDefault();
                }
            });
        });
    }]);
})();


