(function () {
    var app = angular.module('PlausibilityExperimentApp', []);

    app.controller('RenderController', ['$http', '$timeout', function ($http, $timeout) {
        var self = this;
        this.assignmentId = -1;
        this.part = null;
        this.partId = null;
        this.questions = [];
        this.currentChunk = 0;
        this.origin = "";
        this.mail = "";

        this.update = function () {
            var scope = angular.element($("#angularApp")).scope();
            $timeout(function () {
                scope.$apply();
            });
        };

        this.instructionsFinished = function(){
            if(self.origin == "prolific"){
                $('#instructionsSlide').hide();
                $('#workerIdSlide').show();
            }else if (self.origin == "mail"){
                $('#instructionsSlide').hide();
                $('.chunk').first().show();
            }else{
                $('#instructionsSlide').hide();
                $('.chunk').first().show();
            }
        };

        this.nextPicture = function(index){
            var currentSlide = $(".chunk").eq(index);
            currentSlide.hide();
            if(index + 1 < self.questions.length){
                currentSlide.next(".chunk").show();
            }else{
                $(".chunk").hide();
                self.submitResults();
            }
        };

        this.generateRandomId = function()
        {
            var text = "";
            var possible = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

            for( var i=0; i < 30; i++ )
                text += possible.charAt(Math.floor(Math.random() * possible.length));

            return text;
        };

        this.submitResults = function(){
            var workerId = "";
            if(self.origin == "prolific"){
                workerId = self.workerId;
            }else if (self.origin == "mail"){
                self.workerId = "mail_" + self.generateRandomId();
                workerId = self.workerId;
            }else{
                self.workerId = "other_" + self.generateRandomId();
                workerId = self.workerId;
            }

            var answerList = [];

            for(var i = 0; i < self.questions.length; i++){
                var question = self.questions[i];
                var questionId = question.id;
                var answer = question.answer;
                answerList.push({questionId : questionId, answer: answer});
            }

            var result = {
                experimentType : "PlausibilityExperiment",
                workerId : workerId,
                partId : self.partId,
                answers : answerList
            };

            $http.post("/submitResults", result)
                .success(function () {
                    $("#statistics").show();
                })
                .error(function () {
                    setTimeout(function() { self.submitResults() }, 2000);
                });
        };

        this.nextChunk = function(){
            $("#chunkEnd").hide();
            self.currentChunk = self.currentChunk + 1;

            var scope = angular.element($("#angularApp")).scope();
            $timeout(function () {
                scope.$apply();
                var newChunk = $(".chunk");
                newChunk.show();
                newChunk.find(".pictureSlide").first().show();
            });
        };

        this.setAnswer = function (event,question,answer) {
            if(answer > 0 && answer <= 5){
                question.answer = answer;

                var domE = $(event.target);
                var panelBody = domE.closest(".panel-body");
                var button = panelBody.find("button");
                button.prop("disabled", false);
            }
        };

        /*
         *	Taken from: http://stackoverflow.com/questions/20789373/shuffle-array-in-ng-repeat-angular
         *	-> Fisher�Yates shuffle algorithm
         */
        this.shuffleArray = function (array) {
            var m = array.length, t, i;

            // While there remain elements to shuffle
            while (m) {
                // Pick a remaining element�
                i = Math.floor(Math.random() * m--);

                // And swap it with the current element.
                t = array[m];
                array[m] = array[i];
                array[i] = t;
            }

            return array;
        };

        $(document).ready(function () {
            self.origin = $("#origin").val();

            var partId = $("#partId").val();
            self.partId = partId;
            if (partId != "") {
                $http.get("/returnPart?partId=" + partId).success(function (data) {
                    var json = data;
                    self.part = json;
                    self.shuffleArray(json.questions);
                    self.questions = json.questions;

                    for(var i = 0; i < self.questions.length; i++){
                        self.questions[i].answer = 0;
                    }

                    var scope = angular.element($("#angularApp")).scope();
                    $timeout(function () {
                        scope.$apply();
                    });

                    $(document).on("input", ".textInput", function () {
                        if ($(this).val() != "") {
                            $(this).next().removeAttr("disabled");
                        } else {
                            $(this).next().attr("disabled", "disabled");
                        }
                    });

                    $(document).on("click","#submitButton", function(){
                        if(self.origin == "prolific"){
                            window.location.href = "https://prolificacademic.co.uk/submissions/563a0647be9cac000faaa4c9/complete?cc=L2QAYPD9";
                        }else if (self.origin == "mail"){
                            $http.get("/submitMailAddress?mailAddress=" + encodeURIComponent(self.mail) + "&workerId=" + encodeURIComponent(self.workerId)).success(function () {
                                $("#statistics").hide();
                                $("#mailSuccess").show();
                            });
                        }
                    });

                    $("#workerIdButton").click(function () {
                        $("#workerIdSlide").hide();

                        $('.chunk').first().show();
                    });

                    $(window).keydown(function(event){
                        if(event.keyCode == 13) {
                            event.preventDefault();
                            return false;
                        }
                    });

                });
            }
        });

    }]);
})();
