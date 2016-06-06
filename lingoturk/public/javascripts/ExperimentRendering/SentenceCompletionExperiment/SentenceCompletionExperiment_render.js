(function () {
    var app = angular.module('SentenceCompletionExperimentApp', []);

    app.controller('RenderController', ['$http', '$timeout', function ($http, $timeout) {
        var self = this;
        this.assignmentId = -1;
        this.part = null;
        this.questions = [];
        this.currentChunk = 0;
        this.origin = "";
        this.mail = "";
        this.workerId = "";
        this.expId = "";

        self.startExperiment = function () {
            $("#workerIdSlide").hide();
            self.load();
        };

        this.nextPicture = function (index) {
            var currentSlide = $(".chunk").eq(index);
            currentSlide.hide();
            if (index + 1 < self.questions.length) {
                currentSlide.next(".chunk").show();
            } else {
                $(".chunk").hide();
                self.submitResults();
            }
        };

        this.generateRandomId = function () {
            var text = "";
            var possible = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

            for (var i = 0; i < 30; i++)
                text += possible.charAt(Math.floor(Math.random() * possible.length));

            return text;
        };

        this.submitResults = function () {
            var workerId = $("#workerId").val();

            var partId = self.part.id;
            var answerList = [];

            for (var i = 0; i < self.questions.length; i++) {
                var question = self.questions[i];
                var questionId = question.id;
                var answer = question.answer;
                answerList.push({questionId: questionId, answer: answer});
            }

            var result = {
                experimentType : "SentenceCompletionExperiment",
                workerId: workerId,
                partId: partId,
                answers: answerList
            };

            $http.post("/submitResults", result)
                .success(function () {
                    $("#statistics").show();
                })
                .error(function () {
                    setTimeout(function () {
                        self.submitResults()
                    }, 2000);
                });
        };

        /*
         *	Taken from: http://stackoverflow.com/questions/20789373/shuffle-array-in-ng-repeat-angular
         *	-> Fisher?Yates shuffle algorithm
         */
        this.shuffleArray = function (array) {
            var m = array.length, t, i;

            // While there remain elements to shuffle
            while (m) {
                // Pick a remaining element?
                i = Math.floor(Math.random() * m--);

                // And swap it with the current element.
                t = array[m];
                array[m] = array[i];
                array[i] = t;
            }

            return array;
        };

        this.load = function(){
            $http.get("/getPart?expId=" + self.expId + "&workerId=" + self.workerId).success(function (data) {
                var json = data;
                self.part = json;
                self.shuffleArray(self.part.questions);
                self.questions = self.part.questions;

                var scope = angular.element($("#angularApp")).scope();
                $timeout(function () {
                    scope.$apply();
                    $(".chunk").first().show();
                });
                $(".chunk").first().show();

            });
        };

        $(document).ready(function () {
                self.expId = $("#expId").val();
                $(document).on("input", ".textInput", function () {
                    if ($(this).val() != "") {
                        $(this).next("button").removeAttr("disabled");
                        $(this).next().next("button").removeAttr("disabled");
                        $(this).closest("div").find("button").removeAttr("disabled");
                    } else {
                        $(this).next("button").attr("disabled", "disabled");
                        $(this).next().next("button").attr("disabled", "disabled");
                        $(this).closest("div").find("button").attr("disabled", "disabled");
                    }
                });

                $(document).on("click", "#submitButton", function () {
                    window.location.href = "https://prolificacademic.co.uk/submissions/5646062cd01e6b001272744d/complete?cc=SXL0UU2I";
                });
        });
    }]);
})();


