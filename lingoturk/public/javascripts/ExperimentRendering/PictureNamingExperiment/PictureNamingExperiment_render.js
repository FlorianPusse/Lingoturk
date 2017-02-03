(function () {
    var app = angular.module('PictureNamingExperimentApp', []);

    app.controller('RenderController', ['$http', '$timeout', function ($http, $timeout) {
        var self = this;
        this.assignmentId = -1;
        this.questions = null;
        this.chunks = [];
        this.currentChunk = 0;
        this.origin = "";
        this.mail = "";

        this.update = function () {
            var scope = angular.element($("#angularApp")).scope();
            $timeout(function () {
                scope.$apply();
            });
        };

        this.instructionsFinished = function () {
            if (self.origin == "PROLIFIC") {
                $('#instructionsSlide').hide();
                $('#workerIdSlide').show();
            } else if (self.origin == "mail") {
                $('#instructionsSlide').hide();
                $('.pictureSlide').first().show();
            } else {
                $('#instructionsSlide').hide();
                $('.pictureSlide').first().show();
            }
        };

        this.nextPicture = function (index) {
            var currentSlide = $(".chunk").find(".pictureSlide").eq(index);
            currentSlide.hide();
            if (index + 1 < self.chunks[self.currentChunk].pictures.length) {
                currentSlide.next(".pictureSlide").show();
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
            var currChunk = self.chunks[self.currentChunk];

            var workerId = "";
            if (self.origin == "PROLIFIC") {
                workerId = self.workerId;
            } else if (self.origin == "mail") {
                self.workerId = "mail_" + self.generateRandomId();
                workerId = self.workerId;
            } else {
                self.workerId = "other_" + self.generateRandomId();
                workerId = self.workerId;
            }

            var partId = self.part.id;
            var chunkId = currChunk.id;
            var answerList = [];

            for (var i = 0; i < currChunk.pictures.length; i++) {
                var pictureObject = currChunk.pictures[i];
                var pictureId = pictureObject.id;
                var answer = pictureObject.answer;
                answerList.push({pictureId: pictureId, answer: answer});
            }

            var result = {
                experimentType: "PictureNamingExperiment",
                workerId: workerId,
                partId: partId,
                chunkId: chunkId,
                answers: answerList
            };

            $http.post("/submitResults", result)
                .success(function () {
                    if (self.currentChunk + 1 < self.chunks.length) {
                        $("#chunkEnd").show();
                    } else {
                        $("#statistics").show();
                    }
                })
                .error(function () {
                    setTimeout(function () {
                        self.submitResults()
                    }, 2000);
                });
        };

        this.nextChunk = function () {
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

            var questionId = $("#questionId").val();
            var importantChunkId = parseInt($("#importantChunk").val());

            if (questionId != "") {
                $http.get("/getQuestion/" + questionId).success(function (data) {
                    self.part = {number : 1, id : -1};

                    var json = data;
                    self.questions = json;
                    self.shuffleArray(self.questions);
                    self.chunks = [];
                    self.chunks.push(self.questions);

                    var scope = angular.element($("#angularApp")).scope();
                    $timeout(function () {
                        scope.$apply();
                        $(".chunk").first().show();
                    });

                    $(document).on("input", ".textInput", function () {
                        if ($(this).val() != "") {
                            $(this).next().removeAttr("disabled");
                        } else {
                            $(this).next().attr("disabled", "disabled");
                        }
                    });

                    $(document).on("click", "#submitButton", function () {
                        if (self.origin == "PROLIFIC") {
                            window.location.href = "https://prolificacademic.co.uk/submissions/5627688addff3c000dbcdb69/complete?cc=QJVQYV7U";
                        } else if (self.origin == "mail") {
                            $http.get("/submitMailAddress?mailAddress=" + encodeURIComponent(self.mail) + "&workerId=" + encodeURIComponent(self.workerId)).success(function () {
                                $("#statistics").hide();
                                $("#mailSuccess").show();
                            });
                        }
                    });

                    $("#workerIdButton").click(function () {
                        $("#workerIdSlide").hide();

                        $('.pictureSlide').first().show();
                    });

                    $(document).on("keypress", ":input:not(textarea)", function(event) {
                        if (event.keyCode == 13) {
                            event.preventDefault();
                        }
                    });
                });
            }
        });

    }]);
})();
