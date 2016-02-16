(function () {
    var app = angular.module('DC_DND', []);

    app.controller('QuestionController', ['$http', '$timeout', function ($http, $timeout) {
        var self = this;

        this.id = -1;
        this.name = "";
        this.nameOnAmt = "";
        this.description = "";
        this.additionalExplanations = "<pre>To complete this HIT you have to do 4 simple tasks, namely answering a yes/no-question and rephrasing a sentence .\n\nWhen you've done a task, press the continue/submit button.\n\nYou have 7 minutes to do the task before the HIT expires.\n</pre>\n ";
        this.exampleQuestions = [];
        this.parts = [];
        this.type = "LinkingExperimentV1";
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


        this.loadCheaterDetectionQuestions = function (event) {
            return false;

           if($(event.target).val() != ""){
               var files = event.target.files;
               for(var i = 0; i < files.length; i++){
                   var file = files[i];
                   var reader = new FileReader();
                   reader.onload = function(f){
                       var content = f.target.result;
                       self.cheaterDetectionQuestions.push(new self.Part(file.name,content));
                   };
                   reader.readAsText(files[i]);
               }

               $timeout(function () {
                   angular.element($("#angularApp")).scope().$apply();
                   $("#allquestions").accordion("refresh");
                   $("button").button();
                   $timeout(function () {
                       angular.element($("#angularApp")).scope().$apply();
                       $("#allquestions").accordion("refresh");
                       $("button").button();
                   });
               });
           }

            $timeout(function () {
                angular.element($("#angularApp")).scope().$apply();
                $("#allquestions").accordion("refresh");
                $("button").button();
            });
        };

        this.CheaterDetectionQuestion = function(content){
            var self = this;
            self.type = "LinkingQuestion";
            self.content = content;
        };

        this.returnPart = function(fileName){
            var parts = self.parts;
            for(var i = 0; i < parts.length; i++){
                if(parts[i].fileName == fileName){
                    return parts[i];
                }
            }
            var part = new self.Part(fileName);
            parts.push(part);
            return part;
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
                            if(fileName.startsWith("_outlier_")){
                                var filename = fileName.replace("_outlier_","");
                                var part = self.returnPart(filename);
                                part.lhs = content;
                            }else if(fileName.startsWith("rhs_")){
                                var filename = fileName.replace("rhs_","");
                                var part = self.returnPart(filename);
                                part.rhs = content;
                            }else{
                                alert("Wrong file naming");
                            }
                        }
                    })(file.name);
                    reader.readAsText(files[i]);
                }

                $timeout(function () {
                    angular.element($("#angularApp")).scope().$apply();
                    $("#allquestions").accordion("refresh");
                    $("button").button();
                    $timeout(function () {
                        angular.element($("#angularApp")).scope().$apply();
                        $("#allquestions").accordion("refresh");
                        $("button").button();
                    });
                });
            }

            $timeout(function () {
                angular.element($("#angularApp")).scope().$apply();
                $("#allquestions").accordion("refresh");
                $("button").button();
            });
        };

        this.resetParts = function () {
            this.parts = [];
            $timeout(function () {
                angular.element($("#angularApp")).scope().$apply()
            });
        };

        this.Part = function (fileName) {
            var self = this;
            self.type = "PoolGroup";
            self.fileName = fileName;
            self.lhs = "";
            self.rhs = "";
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

            $("input[type=file]").change(function(event){
                self.loadCheaterDetectionQuestions(event);
            });

            // Create Tabs
            $("#tabs").tabs({
                //heightStyle: "content"
            });


            $("#allquestions").accordion({
                heightStyle: "content"
            });

            // Make input element invisible and a button to trigger file dialogue -> better for styling

            //$("input[type='file']").hide();
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

        });

    }]);

})();