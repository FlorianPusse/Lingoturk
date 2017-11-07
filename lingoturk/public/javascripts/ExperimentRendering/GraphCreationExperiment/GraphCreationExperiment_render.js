(function () {
    var app = angular.module('GraphCreationExperimentApp', ["Lingoturk"]);

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
        self.unansweredQuestions = [];
        self.currentQuestion = null;

        self.shuffleQuestions = false;
        self.shuffleSublists = true;
        self.useGoodByeMessage = true;
        self.useStatistics = false;

        self.readThemAll = false;

        self.wait = 5;
        self.maxN = 20;
        self.nrAnswers = 0;

        self.statistics = [
            {name : "Age", type: "number", answer : undefined},
            {name : "Gender", type: "text", answer : ""},
            {name : "Nationality", type: "text", answer : ""},
            {name : "Mother's first language", type: "text", answer : ""},
            {name : "Father's first language", type: "text", answer : ""},
            {name : "Are you bilingual (grown up with more than one language)?", type: "boolean", answer : undefined},
            {name : "Please list the languages you speak at at the advance level.", type: "text", answer : "", optional : true}
        ];

        this.startWait = function(){
            $timeout(function() { if(self.wait > 0){ self.wait--; self.startWait()} }, 1000);
        };

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

        this.randomInt = function(min,max){
            min = Math.ceil(min);
            max = Math.floor(max);
            return Math.floor(Math.random() * (max - min)) + min;
        };

        self.reAskQuestion = null;
        this.resetUnansweredQuestions = function(){
            var labels = [];

            self.unansweredQuestions = [];
            for(var i = 0; i < self.questions.length; ++i){
                if(!labels.contains(self.questions[i].label.trim())){
                    labels.push(self.questions[i].label.trim());
                }
                self.unansweredQuestions.push(self.questions[i].label.trim() + "$$" + self.questions[i]._optionType);
            }

            // set question to ask again
            self.reAskQuestion = self.unansweredQuestions[self.randomInt(0,self.unansweredQuestions.length - 4)];


            var n = self.maxN;
            var m = labels.length;
            var first = (m > n) ? self.randomInt(0,m-n) : 0;
            var startingLabel = labels[first];

            for(var i = 0; i < self.unansweredQuestions.length;++i){
                if(self.unansweredQuestions[i].split("$$")[0] === startingLabel){
                    self.currentQuestion = self.getQuestion(self.unansweredQuestions[i]);
                    self.unansweredQuestions.splice(i,1);
                    break;
                }
            }
        };

        this.getQuestion = function(id){
            var label = id.split("$$")[0];
            var optionType = id.split("$$")[1];
            for(var i = 0; i < self.questions.length; ++i){
                if(self.questions[i].label == label && self.questions[i]._optionType == optionType){
                    return self.questions[i];
                }
            }
            console.log("We shouldn't end up here.");
        };

        this.nextSublist = function(){
            self.questionIndex = 0;
            self.questions = self.subListMap[self.subListsIds[0]];
            self.resetUnansweredQuestions();
            self.currentQuestion = self.getQuestion(self.unansweredQuestions[0]);
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
                experimentType : "GraphCreationExperiment",
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

        this.checkAnswer = function(answer){
            for (var k in answer){
                if(answer.hasOwnProperty(k)){
                    if(answer[k]){
                        return true;
                    }
                }
            }
            return false;
        };

        this.nextQuestion = function(){
            self.nrAnswers++;

            if(self.reAskQuestion != null && self.maxN - self.nrAnswers == 3){
                var question = self.getQuestion(self.reAskQuestion);
                self.reAskQuestion = null;
                var copiedQuestion = jQuery.extend(true, {}, question);
                copiedQuestion.answer = {};
                copiedQuestion.answer.reAsk = true;
                self.questions.push(copiedQuestion);
                self.currentQuestion = copiedQuestion;
                return;
            }

            if(self.unansweredQuestions.length > 0 && self.nrAnswers < self.maxN){
                var positiveAnswers = [];
                for(var key in self.currentQuestion.answer){
                    if(self.currentQuestion.answer.hasOwnProperty(key)){
                        var answer = self.currentQuestion.answer[key];
                        if(answer){
                            positiveAnswers.push(key);
                        }
                    }
                }
                if(positiveAnswers.length == 0){
                    var index = self.randomInt(0,self.unansweredQuestions.length);
                    self.currentQuestion = self.getQuestion(self.unansweredQuestions[index]);
                    self.unansweredQuestions.splice(index,1);
                }else{
                    var found = false;
                    do {
                        var nextLabel = randomElement(positiveAnswers);
                        positiveAnswers.splice(positiveAnswers.indexOf(nextLabel),1);
                        for (var i = 0; i < self.unansweredQuestions.length; ++i) {
                            if (self.unansweredQuestions[i].trim() == (nextLabel.trim() + "$$" + self.currentQuestion._optionType)) {
                                found = true;
                                self.currentQuestion = self.getQuestion(self.unansweredQuestions[i]);
                                self.unansweredQuestions.splice(i,1);
                                break;
                            }
                        }
                        for (var i = 0; i < self.unansweredQuestions.length; ++i) {
                            if (self.unansweredQuestions[i].split("$$")[0] == nextLabel.trim()) {
                                found = true;
                                self.currentQuestion = self.getQuestion(self.unansweredQuestions[i]);
                                self.unansweredQuestions.splice(i,1);
                                break;
                            }
                        }
                    } while(!found && positiveAnswers.length > 0);
                    if(!found){
                        var alternativeVersion = true;
                        if(alternativeVersion){
                            var index = self.randomInt(0,self.unansweredQuestions.length);
                            self.currentQuestion = self.getQuestion(self.unansweredQuestions[index]);
                            self.unansweredQuestions.splice(index,1);
                        }else{
                            // None of the chosen answers have to be answered anymore -> try to get at least a question form the same answer list
                            for (var i = 0; i < self.unansweredQuestions.length; ++i){
                                var id = self.unansweredQuestions[i];
                                var label = id.split("$$")[0];
                                if (label == self.currentQuestion.label){
                                    self.currentQuestion = self.getQuestion(id);
                                    self.unansweredQuestions.splice(i,1);
                                    return;
                                }
                            }

                            // None of the chosen answers have to be answered anymore -> return any question
                            self.currentQuestion = self.getQuestion(self.unansweredQuestions[0]);
                            self.unansweredQuestions.splice(0,1);
                        }
                    }
                }
            }else{
                self.next();
            }
        };

        this.load = function(callback){
            var subListMap = self.subListMap;

            if(self.questionId != null){
                $http.get("/getQuestion/" + self.questionId).success(function (data) {
                    self.questions = [data];

                    subListMap[self.questions[0].subList] = [self.questions[0]];

                    if(callback !== undefined){
                        callback();
                    }
                });
            }else if(self.partId != null){
                $http.get("/returnPart?partId=" + self.partId).success(function (data) {
                    var json = data;
                    self.part = json;
                    self.questions = json.questions;

                    if(self.shuffleQuestions){
                        shuffleArray(self.part.questions);
                    }

                    for(var i = 0; i < self.questions.length; ++i){
                        var q = self.questions[i];
                        q._optionType = q.optionList;
                        q.label = q.label.trim();
                        q.options = eval(q.options);
                        q.descriptions = eval(q.descriptions);
                        var index = q.options.indexOf(q.label.trim());
                        if (index > -1){
                            q.options.splice(index,1);
                            q.descriptions.splice(index,1);
                        }
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
                    self.resetUnansweredQuestions();

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

                    if(self.shuffleQuestions){
                        shuffleArray(self.part.questions);
                    }

                    for(var i = 0; i < self.questions.length; ++i){
                        var q = self.questions[i];
                        q._optionType = q.optionList;
                        q.label = q.label.trim();
                        q.descriptions = eval(q.descriptions);
                        var index = q.options.indexOf(q.label.trim());
                        if (index > -1){
                            q.options.splice(index,1);
                            q.descriptions.splice(index,1);
                        }
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
                    self.resetUnansweredQuestions();

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

            self.allStates = ["instructionsSlide","instructions1Slide","workerIdSlide","statisticsSlide","questionSlide"];

            if(!self.useStatistics){
                var index = self.allStates.indexOf("statisticsSlide");
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


