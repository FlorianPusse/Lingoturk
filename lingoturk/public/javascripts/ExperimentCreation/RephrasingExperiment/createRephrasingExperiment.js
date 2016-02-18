(function () {
    var app = angular.module('DC_DND', []);

    app.controller('QuestionController', ['$http', '$timeout', function ($http, $timeout) {
        var self = this;

        this.id = -1;
        this.name = "";
        this.nameOnAmt = "";
        this.description = "";
        this.additionalExplanations = "<pre>To complete this HIT you have to do 4 simple tasks, namely answering a yes/no-question and rephrasing a sentence. Note that you won't be able to go back to the original sentence at the time of answering the question or rephrasing, so please read the sentence attentively.</pre>";
        this.exampleQuestions = [
            {sentence: "Before Peter went to the store, he took his purse that was very old and almost falling apart.", question: "Did Peter remember to take his purse when going to the store?", restatement: "Peter took his purse. His purse was very old and almost falling apart. He then went to the store.", answer : true, type: "NewExpQuestion"}
        ];
        this.type = "RephrasingExperiment";
        this.parts = [];

        this.submitExperiment = function () {
            //self.prepareQuestions();

            var experiment = {
                id: this.id,
                name: this.name,
                nameOnAmt: this.nameOnAmt,
                description: this.description,
                additionalExplanations: this.additionalExplanations,
                type : this.type,
                exampleQuestions: this.exampleQuestions,
                parts: this.parts
            };

            $http.post("/submitNew_Experiment", experiment)
                .success(function () {
                    alert("Success!");
                })
                .error(function (data) {
                    alert("Error!");
                });
        };

        this.addQuestion = function (part) {
            if (part.newSentence1 !== "" && part.newQuestion1 !== "" && part.newSentence2 !== "" && part.newQuestion2 !== "") {
                part.questions.push(new this.Question(part.newId, part.newSentence, part.newQuestion));
                part.newSentence1 = "";
                part.newQuestion1 = "";
                part.newSentence2 = "";
                part.newQuestion2 = "";
            }
        };

        this.addQuestion_param = function (partNr, newId, newSentence, newQuestion) {
            if (newId != "" && newSentence !== "" && newQuestion !== "") {
                var part = this.parts[partNr];

                part.questions.push(new this.Question(newId, newSentence, newQuestion));
                part.newId = "";
                part.newSentence = "";
                part.newQuestion = "";
            }
        };

        this.loadQuestions = function () {
            if ($("#fileQuestions").val() != "") {
                var file = document.getElementById('fileQuestions');
                var reader = new FileReader();
                var content = "";


                reader.onload = function (ready) {
                    content = CSVToArray(ready.target.result);
                    var scope = angular.element($("#angularApp")).scope();

                    var act_part = new self.Part();

                    $.each(content, function () {
                        if (this[0] == "newPart") {
                            self.parts.push(act_part);
                            act_part = new self.Part();
                        }
                        else if (this[0] !== undefined && this[1] !== undefined && this[2] !== undefined && this[3] !== undefined && this[4] !== undefined && this[5] !== undefined) {
                            var newSentence1 = this[0];
                            var newQuestion1 = this[1];
                            var questionFirst1 = this[2];
                            var newSentence2 = this[3];
                            var newQuestion2 = this[4];
                            var questionFirst2 = this[5];
                            act_part.questions.push(new self.Question(newSentence1, newQuestion1, questionFirst1, newSentence2, newQuestion2, questionFirst2));
                        }

                        $timeout(function () {
                            angular.element($("#angularApp")).scope().$apply();
                            $("#allquestions").accordion("refresh");
                            $("button").button();
                        });
                    });

                    if (act_part.questions.length != 0) {
                        self.parts.push(act_part);
                    }

                    $timeout(function () {
                        angular.element($("#angularApp")).scope().$apply();
                        $("#allquestions").accordion("refresh");
                        $("button").button();
                    });

                };

                reader.readAsText(file.files[0]);
                $("#fileQuestions").val("");
            }

        };

        this.resetPart = function (part) {
            part.questions = [];
            $timeout(function () {
                angular.element($("#angularApp")).scope().$apply()
            });
        };

        this.removeQuestion = function (part, question) {
            var index = part.questions.indexOf(question);
            part.questions.splice(index, 1);
        };

        this.resetParts = function () {
            this.parts = [];
            $timeout(function () {
                angular.element($("#angularApp")).scope().$apply()
            });
        };

        this.newPart = function () {
            this.parts.push(new this.Part());
            $timeout(function () {
                angular.element($("#angularApp")).scope().$apply();
                $("#allquestions").accordion("refresh");
                $("#allquestions").accordion("option", "active", -1);
                $("button").button();
            });
        };

        this.removePart = function (part) {
            var index = this.parts.indexOf(part);
            this.parts.splice(index, 1);
        };

        this.Question = function (sentence1, question1, questionFirst1, sentence2, question2, questionFirst2) {
            var self = this;
            self.type = "RephrasingQuestion";
            self.sentence1 = sentence1;
            self.question1 = question1;
            self.questionFirst1 = questionFirst1;
            self.sentence2 = sentence2;
            self.question2 = question2;
            self.questionFirst2 = questionFirst2;
        };

        this.Part = function () {
            var self = this;
            self.type = "DisjointGroup";
            self.questions = [];
            self.newSentence1 = "";
            self.newQuestion1 = "";
            self.newSentence2 = "";
            self.newQuestion2 = "";
        };

        this.init = function () {
            if ($("#experimentID").length) {
                var id = $("#experimentID").val();
                $http.get("/returnJson/" + id).success(function (data) {
                    self.id = data.id;
                    self.name = data.name;
                    self.nameOnAmt = data.nameOnAmt;
                    self.description = data.description;
                    self.additionalExplanations = data.additionalExplanations;
                    self.exampleQuestions = data.exampleQuestions;
                    self.parts = data.parts;
                    $timeout(function () {
                        angular.element($("#angularApp")).scope().$apply();
                        $("#allquestions").accordion("refresh");
                        $("button").button();
                    });
                });
                $timeout(function () {
                    angular.element($("#angularApp")).scope().$apply();
                    $("#allquestions").accordion("refresh");
                    $("button").button();
                });
            }
        };
        this.init();

        $(document).ready(function () {

            // Create Tabs
            $("#tabs").tabs({
                //heightStyle: "content"
            });


            $("#allquestions").accordion({
                heightStyle: "content"
            });

            // Make input element invisible and a button to trigger file dialogue -> better for styling

            $("input[type='file']").hide();
            $(".invisible").hide();

            $("#QuestionCSVButton").click(function () {
                $("#fileQuestions").trigger('click');
                return false;
            });

            $("#fileQuestions").change(function () {
                $("#templateTriggerQuestions").trigger('click');
                return false;
            });

            $("#CDQuestionCSVButton").click(function () {
                $("#fileCDQuestions").trigger('click');
                return false;
            });

            $("#fileCDQuestions").change(function () {
                $("#templateTriggerCDQuestions").trigger('click');
                return false;
            });

            // Create button

            $("button").button();

            // Show Trash Symbol
            $(document).on("mouseenter", ".question", function () {
                $(this).find("td").last().find("img").fadeTo('fast', 1);
            });
            $(document).on("mouseleave", ".question", function () {
                $(this).find("td").last().find("img").fadeTo('fast', 0.01);
            });

            // Trash Symbol to delete words
            $(document).on("mouseenter", ".word", function () {
                $(".word").css('cursor', "url('/assets/images/trash_curser_icon.gif'), auto");
            });
            $(document).on("mouseleave", ".word", function () {
                $(".word").css('cursor', 'auto');
            });

            // Create Dialog
            $("#dialog").dialog({
                autoOpen: false,
                width: 400,
                buttons: [
                    {
                        text: "Ok",
                        click: function () {
                            $(this).dialog("close");
                            $("#submitButton").click();
                        }
                    },
                    {
                        text: "Cancel",
                        click: function () {
                            $(this).dialog("close");
                        }
                    }
                ]
            });

            $("#dialogButton").click(function (event) {
                $("#dialog").dialog("open");
                event.preventDefault();
            });

        });

    }]);

})();