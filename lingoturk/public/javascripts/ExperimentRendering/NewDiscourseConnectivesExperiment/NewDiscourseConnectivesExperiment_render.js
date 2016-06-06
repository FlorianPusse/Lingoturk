(function () {
    var app = angular.module('NewDiscourseConnectivesExperimentApp', ['Lingoturk']);

    app.controller('RenderController', ['$http', '$timeout', function ($http, $timeout) {
        var self = this;
        this.partId = null;
        this.workerId = "";
        this.questionIndex = 0;

        /*
         *	Taken from: http://stackoverflow.com/questions/20789373/shuffle-array-in-ng-repeat-angular
         *	-> Fisherâ€“Yates shuffle algorithm
         */
        this.shuffleArray = function (array) {
            var m = array.length, t, i;

            // While there remain elements to shuffle
            while (m) {
                // Pick a remaining elementâ€¦
                i = Math.floor(Math.random() * m--);

                // And swap it with the current element.
                t = array[m];
                array[m] = array[i];
                array[i] = t;
            }

            return array;
        };

        this.finished = function () {
            self.questionIndex++;

            if(self.questionIndex >= self.questions.length){
                var answerList = [];

                for(var i = 0; i < self.questions.length; i++){
                    var q = self.questions[i];
                    answerList.push(q.answers);
                }

                var result = {
                    experimentType : "NewDiscourseConnectivesExperiment",
                    workerId: self.workerId,
                    partId: self.partId,
                    answers : answerList
                };

                $http.post("/submitResults", result)
                    .success(function () {
                        window.location.href = "https://prolificacademic.co.uk/submissions/5732eb0d961cde000d7a2857/complete?cc=FPEQXRKR";
                    })
                    .error(function () {
                        setTimeout(function () {
                            self.submitting = false;
                            self.finished();
                        }, 2000);
                    });
            }
        };

        $(document).ready(function () {
            var partId = $("#partId").val();
            self.partId = partId;
            if (partId != "") {
                $http.get("/returnPart?partId=" + partId).success(function (data) {
                    var json = data;
                    self.part = json;
                    self.questions = json.questions;

                    for(var i = 0; i < self.questions.length; ++i){
                        self.questions[i].answers = {questionId: self.questions[i].id, connective1 : "", connective2: "", manualAnswer1: "", manualAnswer2: ""}
                    }
                });
            }
            $(document).on("keypress", ":input:not(textarea)", function(event) {
                if (event.keyCode == 13) {
                    event.preventDefault();
                }
            });
        });
    }
    ])
    ;
})();