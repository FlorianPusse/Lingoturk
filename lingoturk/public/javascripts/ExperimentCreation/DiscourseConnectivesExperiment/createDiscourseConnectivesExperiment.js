(function () {
    var app = angular.module('DC_DND', []);

    app.controller('QuestionController', ['$http', '$timeout', function ($http, $timeout) {
        var self = this;

        this.id = -1;
        this.name = "";
        this.nameOnAmt = "";
        this.description = "";
        this.type = "DiscourseConnectivesExperiment";
        this.additionalExplanations = "<pre>Take a look at the two sentences below. They have a logical connection between them that can be expressed by one of the connective words in the list of connectives above the sentences. You have two tasks to perform:\n\n1. Drag ONE connective into the green box (the target box) between the sentences. Choose the one that best fits the logical relation between the sentences. This will appear in the top right corner of the box.\n\nThe list of connectives will immediately be replaced by another list with a light orange background.\nThe target box will be replaced by THREE target boxes.\n\n2. Now drag EACH connective word from the upper box to the relevant target box that you think is suitable to connect the two sentences by logical relations that make sense to you.\n\nThe meaning of the boxes:\n\"not relevant\" -- these connectives do not clearly logically connect the two sentences.\n\"valid connective\" -- these connectives represent a logical link between the two sentences.\n\"can't decide\" -- you cannot decide whether any of these connectives represent a logical link between the sentences\n\n3. When you've classified EACH of the connectives into the target boxes press the submit button."
        + "\n\nNOTE: You can change your mind at each step. To undo the connective selection in step 1, there is an X at the right corner of the target box. To undo a connective selection in step 2, just drag the connective word back into the upper box. If you press the X next to the selected connective from step 1 DURING step 2, you start over at step 1.\n\nYou have 10 minutes to do the task before the HIT expires.</pre>\n ";

        this.exampleQuestions = [
            {sentence1: "", sentence2: "", connectives: "", possibleConnectives: "", type: "DNDQuestion"},
            {sentence1: "", sentence2: "", connectives: "", possibleConnectives: "", type: "DNDQuestion"},
            {sentence1: "", sentence2: "", connectives: "", possibleConnectives: "", type: "DNDQuestion"}
        ];

        this.parts = [];

        this.cheaterDetectionQuestions = [];
        this.newCDSentence1 = "";
        this.newCDSentence2 = "";
        this.newCDSentenceType = "";

        this.submitExperiment = function () {
            var experiment = {
                id : this.id,
                name: this.name,
                nameOnAmt: this.nameOnAmt,
                description: this.description,
                additionalExplanations: this.additionalExplanations,
                exampleQuestions: this.exampleQuestions,
                parts: this.parts,
                type : this.type,
                cheaterDetectionQuestions: this.cheaterDetectionQuestions
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
            if (part.newSentence1 !== "" && part.newSentence2 !== "" && part.newSentenceType) {
                part.questions.push(new this.Question(part.newId, part.newSentence1, part.newSentence2, part.newSentenceType));
                part.newId = "";
                part.newSentence1 = "";
                part.newSentence2 = "";
                part.newSentenceType = "";
                $(".selectable").last().selectable({
                    selected: function (event, ui) {
                        ui.selected.remove();
                    }
                });
            }
        };

        this.addQuestion_param = function (partNr, newId, newSentence1, newSentence2, sentenceType) {
            if (newId != "" && newSentence1 !== "" && newSentence2 !== "" && sentenceType !== "") {
                var part = this.parts[partNr];

                part.questions.push(new this.Question(newId, newSentence1, newSentence2));
                part.newId = "";
                part.newSentence1 = "";
                part.newSentence2 = "";
                part.sentenceType = "";
                $(".selectable").last().selectable({
                    selected: function (event, ui) {
                        ui.selected.remove();
                    }
                });
            }
        };

        this.resetPart = function (part) {
            part.questions = [];
            $timeout(function () {
                angular.element($("#angularApp")).scope().$apply()
            });
        };

        this.resetCheaterDetectionQuestions = function () {
            this.cheaterDetectionQuestions = [];
            $timeout(function () {
                angular.element($("#angularApp")).scope().$apply()
            });
        };

        this.addCheaterDetectionQuestion = function () {
            if (this.newCDSentence1 !== "" && this.newCDSentence2 !== "" && this.newProposedConnectives !== "" && this.newMustNotHaveConnectives != "" && this.newCDSentenceType != "") {
                this.cheaterDetectionQuestions.push(
                    new this.CheaterDetectionQuestion(
                        this.newCDSentence1,
                        this.newCDSentence2,
                        this.newProposedConnectives.split(","),
                        this.newMustNotHaveConnectives.split(","),
                        this.newCDSentenceType
                        ));
                this.newCDSentence1 = "";
                this.newCDSentence2 = "";
                this.newProposedConnectives = "";
                this.newMustNotHaveConnectives = "";
                this.newCDSentenceType = "";
                $(".selectable").last().selectable({
                    selected: function (event, ui) {
                        ui.selected.remove();
                    }
                });
            }
        };

        this.addCheaterDetectionQuestion_param = function (Sentence1, Sentence2, proposedConnectives, MustNotHaveConnectives, sentenceType) {
            if (Sentence1 !== "" && Sentence2 !== "" && proposedConnectives !== "" && MustNotHaveConnectives !== "" && sentenceType != "") {
                this.cheaterDetectionQuestions.push(
                    new this.CheaterDetectionQuestion(
                        Sentence1,
                        Sentence2,
                        proposedConnectives,
                        MustNotHaveConnectives,
                        sentenceType));
                $(".selectable").last().selectable({
                    selected: function (event, ui) {
                        ui.selected.remove();
                    }
                });
            }
        };

        this.loadCDQuestions = function () {
            if ($("#fileCDQuestions").val() != "") {
                var file = document.getElementById('fileCDQuestions');
                var reader = new FileReader();
                var content = "";

                reader.onload = function (ready) {
                    content = CSVToArray(ready.target.result);
                    var scope = angular.element($("#angularApp")).scope();

                    $.each(content, function () {
                        if (this[2] !== undefined && this[3] !== undefined && this[4] !== undefined) {
                            var sentence1 = this[0];
                            var sentence2 = this[1];
                            var proposedConnectives = this[2].split(",");
                            var MustNotHaveConnectives = this[3].split(",");
                            var sentenceType = this[4];
                            self.addCheaterDetectionQuestion_param(sentence1, sentence2, proposedConnectives, MustNotHaveConnectives,sentenceType);
                        }
                    });

                    $timeout(function () {
                        angular.element($("#angularApp")).scope().$apply()
                    });
                };

                reader.readAsText(file.files[0]);
                $("#fileCDQuestions").val("");
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
                        else if (this[0] !== undefined && this[1] !== undefined && this[2] !== undefined && this[3] !== undefined) {
                            var newId = this[0];
                            var newSentence1 = this[1];
                            var newSentence2 = this[2];
                            var sentenceType = this[3];
                            act_part.questions.push(new self.Question(newId, newSentence1, newSentence2,sentenceType));
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

        this.removeQuestion = function (part, question) {
            var index = part.questions.indexOf(question);
            part.questions.splice(index, 1);
        };

        this.removeCheaterDetection = function (question) {
            var index = this.cheaterDetectionQuestions.indexOf(question);
            this.cheaterDetectionQuestions.splice(index, 1);
        };

        this.resetParts = function(){
            this.parts = [];
            $timeout(function () {
                angular.element($("#angularApp")).scope().$apply()
            });
        };

        this.removeWord = function (connectives, connective) {
            var index = connectives.indexOf(connective);
            connectives.splice(index, 1);
        };

        this.removeCDWord = function (kindOfWord, quest, word) {
            var connectives;
            if (kindOfWord == 'proposedConnective') {
                connectives = quest.proposedConnectives;
            }
            if (kindOfWord == 'mustNotHaveConnective') {
                connectives = quest.mustNotHaveConnectives;
            }

            var index = connectives.indexOf(word);
            connectives.splice(index, 1);
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

        this.Question = function (id, Sentence1, Sentence2, sentenceType) {
            var self = this;
            self.type = "DiscourseConnectivesQuestion";
            self.id = id;
            self.sentence1 = Sentence1;
            self.sentence2 = Sentence2;
            self.sentenceType = sentenceType;
        };

        this.CheaterDetectionQuestion = function (Sentence1, Sentence2, proposedConnectives, MustNotHaveConnectives, sentenceType) {
            var self = this;
            self.sentence1 = Sentence1;
            self.sentence2 = Sentence2;
            self.proposedConnectives = proposedConnectives;
            self.mustNotHaveConnectives = MustNotHaveConnectives;
            self.sentenceType = sentenceType;
        };

        this.Part = function () {
            var self = this;
            self.type = "DisjointGroup";
            self.questions = [];
            self.newId = "";
            self.newSentence1 = "";
            self.newSentence2 = "";
            self.newSentenceType = "";
        };

        $(document).ready(function () {
            // Create Tabs
            $("#tabs").tabs({
                //heightStyle: "content"
            });


            $("#allquestions").accordion({
                heightStyle : "content"
            });

            // Make input element invisible and a button to trigger file dialogue -> better for styling

            $("input[type='file']").hide();
            $(".invisible").hide();

            $("#QuestionCSVButton").click(function(){
                $("#fileQuestions").trigger('click');
                return false;
            });

            $("#fileQuestions").change(function(){
                $("#templateTriggerQuestions").trigger('click');
                return false;
            });

            $("#CDQuestionCSVButton").click(function(){
                $("#fileCDQuestions").trigger('click');
                return false;
            });

            $("#fileCDQuestions").change(function(){
                $("#templateTriggerCDQuestions").trigger('click');
                return false;
            });

            // Create button

            $("button").button();

            // Show Trash Symbol
            $(document).on("mouseenter", ".question", function () {
                $(this).find("td").last().find("img").fadeTo('fast',1);
            });
            $(document).on("mouseleave", ".question", function () {
                $(this).find("td").last().find("img").fadeTo('fast',0.01);
            });

            // Trash Symbol to delete words
            $(document).on("mouseenter", ".word", function () {
                $(".word").css('cursor', "url('/assets/images/trash_curser_icon.gif'), auto");
            });
            $(document).on("mouseleave", ".word", function () {
                $(".word").css('cursor', 'auto');
            });

            // Create Dialog
            $( "#dialog" ).dialog({
                autoOpen: false,
                width: 400,
                buttons: [
                    {
                        text: "Ok",
                        click: function() {
                            $( this ).dialog( "close" );
                            $("#submitButton").click();
                        }
                    },
                    {
                        text: "Cancel",
                        click: function() {
                            $( this ).dialog( "close" );
                        }
                    }
                ]
            });

            $("#dialogButton").click(function(event){
                $("#dialog").dialog("open");
                event.preventDefault();
            });

            if ($("#experimentID").length) {
                var id = $("#experimentID").val();
                $http.get("/returnJson/" + id).success(function (data) {
                    self.id = data.id;
                    self.name = data.name;
                    self.nameOnAmt = data.nameOnAmt;
                    self.description = data.description;
                    self.additionalExplanations = data.additionalExplanations;
                    self.exampleQuestions = data.exampleQuestions;
                    self.cheaterDetectionQuestions = data.cheaterDetectionQuestions;
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

        });

    }]);

})();