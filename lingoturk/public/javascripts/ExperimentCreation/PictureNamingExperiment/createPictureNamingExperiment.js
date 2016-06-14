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
        this.type = "PictureNamingExperiment";
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

        this.loadQuestions = function () {
            if ($("#fileQuestions").val() != "") {
                var files = document.getElementById('fileQuestions').files;
                for(var i = 0; i < files.length; i++){
                    var file = files[i];
                    var reader = new FileReader();
                    reader.onload = (function(fileName) {
                        return function(f){
                            var content = f.target.result;
                            var parsedContent = CSVToArray(content,'\t');

                            var pictures = [];

                            for(var rowIndex = 0; rowIndex < parsedContent.length; rowIndex++){
                                var row = parsedContent[rowIndex];
                                var picFileName = row[0];
                                var text = row[1];
                                if(picFileName != ""){
                                    pictures.push(new self.Picture(picFileName,text));
                                }
                            }

                            var chunkNr = self.getFileNumber(fileName);
                            var chunk = new self.Chunk(chunkNr,pictures);

                            self.parts[0].questions.push(chunk);
                            self.parts[1].questions.push(chunk);
                            self.parts[2].questions.push(chunk);
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

        this.getFileNumber = function(fileName){
            var character = fileName.charAt(5);
            return parseInt(character);
        };

        this.resetParts = function () {
            self.parts = [];
            self.parts.push(new self.Part(1));
            self.parts.push(new self.Part(2));
            self.parts.push(new self.Part(3));
            $timeout(function () {
                angular.element($("#angularApp")).scope().$apply()
            });
        };

        this.Part = function (number) {
            var self = this;
            self._type = "DisjointGroup";
            self.number = number;
            self.questions = [];
        };

        this.Chunk = function(number,pictures){
            var self = this;
            self._type = "PictureNamingChunk";
            self.number = number;
            self.pictures = pictures;
        };

        this.Picture = function(fileName,text){
            var self = this;
            self.fileName = fileName;
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

            self.parts.push(new self.Part(1));
            self.parts.push(new self.Part(2));
            self.parts.push(new self.Part(3));
        });

    }]);

})();