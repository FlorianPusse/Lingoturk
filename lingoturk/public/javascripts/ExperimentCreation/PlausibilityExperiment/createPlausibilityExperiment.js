(function () {
    var app = angular.module('StoryCompletionExperiment', []);

    app.controller('QuestionController', ['$http', '$timeout', function ($http, $timeout) {
        var self = this;

        this.id = -1;
        this.name = "";
        this.nameOnAmt = "";
        this.description = "";
        this.additionalExplanations = "<pre>To complete this HIT you have to do 4 simple tasks, namely answering a yes/no-question and rephrasing a sentence .\n\nWhen you've done a task, press the continue/submit button.\n\nYou have 7 minutes to do the task before the HIT expires.\n</pre>\n ";
        this.exampleQuestions = [];
        this.parts = [];
        this.type = "PlausibilityExperiment";
        this.cheaterDetectionQuestions = [];

        this.submitExperiment = function () {
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
                .error(function () {
                    alert("Error!");
                });
        };

        this.isUndefined = function(variable){
            if(typeof variable === 'undefined'){
                return true;
            }else{
                return false;
            }
        };

        this.loadQuestions = function () {
            if ($("#fileQuestions").val() != "") {
                var files = document.getElementById('fileQuestions').files;
                for(var i = 0; i < files.length; i++){
                    var file = files[i];
                    var reader = new FileReader();
                    reader.onload = (function(fileName) {
                        return function(f){
                            var content = f.target.result;
                            var parsedContent = CSVToArray(content,',');

                            var questions = [];

                            for(var rowIndex = 0; rowIndex < parsedContent.length; rowIndex++){
                                var row = parsedContent[rowIndex];
                                var number = row[0];
                                var condition = row[1];
                                var text = row[2];
                                if(!(self.isUndefined(number) || self.isUndefined(condition) || self.isUndefined(text) )){
                                    questions.push(new self.Question(number,condition,text));
                                }
                            }

                            self.parts.push(new self.Part(questions));
                        }
                    })(file.name);
                    reader.readAsText(files[i],'UTF-8');
                }

                $timeout(function () {
                    angular.element($("#angularApp")).scope().$apply();
                    $timeout(function () {
                    });
                });
            }

            $timeout(function () {
                angular.element($("#angularApp")).scope().$apply();
            });
        };

        this.resetParts = function () {
            this.parts = [];
            $timeout(function () {
                angular.element($("#angularApp")).scope().$apply()
            });
        };

        this.Part = function (questions) {
            var self = this;
            self.type = "FullGroup";
            self.questions = questions;
        };

        this.Question = function(number,condition,text){
            var self = this;
            self.type = "PlausibilityQuestion";
            self.number = number;
            self.condition = condition;
            self.text = text;
        };

        $(document).ready(function () {

            // Create Tabs
            $("#tabs").tabs({});

            // Make input element invisible and a button to trigger file dialogue -> better for styling

            $(".invisible").hide();

            $("#QuestionCSVButton").click(function () {
                $("#fileQuestions").trigger('click');
                return false;
            });

            $("#fileQuestions").change(function () {
                $("#templateTriggerQuestions").trigger('click');
                return false;
            });
        });

    }]);

})();