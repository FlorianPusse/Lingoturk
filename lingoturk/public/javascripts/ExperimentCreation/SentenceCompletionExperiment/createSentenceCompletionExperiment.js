(function () {
    var app = angular.module('StoryCompletionV2', []);

    app.controller('QuestionController', ['$http', '$timeout', function ($http, $timeout) {
        var self = this;

        this.id = -1;
        this.name = "";
        this.nameOnAmt = "";
        this.description = "";
        this.additionalExplanations = '<strong style="margin-bottom:10px;display: block;">Dear participant,</strong> <ul style="padding-left:17px;padding-right:5px"> <li>This study consists of 30 stories. Every story shows one sentence. Your task is to continue the story, by writing what you think the next sentence will be, in the space provided. You cannot write more than two sentences to complete the stories. <div style="text-align : center ;margin-top:10px;margin-bottom:10px"> The sentences that you write have to be complete sentences, and preferably longer than two or three words. </div> </li> <li>Write the first completion that comes to mind. Don\'t add extra humor or creativity to the task. <strong>We are interested in the most obvious completion that occurs to you.</strong></li> <li>Please treat each item separately -- do not try to tie the different passages together into a longer story.</li> <li>Do not go back and revise earlier continuations.<strong> Please note: if you try to go back in the browser, all your answers will be lost!</strong></li> <li>Below you will find an example of the task: <br/> <label style="margin-top:10px" for="example1">Example 1:</label> <pre id="example2">"The unhappy congressman from the next city over sat by the loud woman in the trailer park." <strong>Possible answer</strong>: <span style="text-decoration : underline ;">He decided to talk to her about unemployment in the state.</span> </pre> <label style="margin-top:10px" for="example1">Example 2:</label> <pre id="example2">"The elderly woman by the lamp post ate next to the happy gentleman wearing a top hat." <strong>Possible answer</strong>: <span style="text-decoration : underline ;">The gentleman politely commented to her that she was chewing too loudly.</span> </pre> </li> </ul> <strong style="text-align : center ;margin-top:20px;display:block;margin-bottom:30px">Once you have read and understood these instructions, click the button below to go to the Story Continuation Study.</strong>';
        this.exampleQuestions = [];
        this.parts = [];
        this.type = "SentenceCompletionExperiment";
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
                            var parsedContent = CSVToArray(content,' ');

                            var stories = [];

                            for(var rowIndex = 0; rowIndex < parsedContent.length; rowIndex++){
                                var row = parsedContent[rowIndex];

                                var itemNr = row[0];
                                var list = row[1];
                                var length = row[2];
                                var type = row[3];
                                var story = row[4];

                                if(typeof story !== 'undefined' && story != ""){
                                    stories.push(new self.Question(itemNr,list,length,type,story));
                                }
                            }
                            self.parts.push(new self.Part(stories));
                        }
                    })(file.name);
                    reader.readAsText(files[i]);
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
            if(typeof list !== 'undefined'){
                self.list = list;
            }

        };

        this.Question = function(itemNr,list,length,type,story){
            var self = this;
            self.type = "SentenceCompletionQuestion";
            self.itemNr = itemNr;
            self.list = list;
            self.story = story;
            self.itemLength = length;
            self.itemType = type;

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