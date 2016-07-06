(function () {
    var app = angular.module('RephrasingExperimentApp', ["Lingoturk"]);

    app.controller('RenderController', ['$http', '$timeout', function ($http, $timeout) {
        var self = this;
        self.questions = [];
        self.questionId = null;
        self.workerId = null;

        this.begin = function(index){
            var date = new Date();
            self.questions[index].readingTime = date.getTime() - self.questions[index].startTime;
            $("#question" + index + " .sentenceSlide").hide();
            $(self.questions[index].order[0]).show();
        };

        this.next = function(index){
            $(self.questions[index].order[0]).hide();
            $(self.questions[index].order[1]).show();
            self.questions[index].lastSlide = true;
        };

        this.statisticsFinished = function(index){
            $("#statisticsSlide1").hide();
            $("#workerSlide").show();
        };

        this.mainInstructionsFinished = function(){
            $("#instructions").hide();
            $("#question0").show();
        };

        this.instructionsFinished = function(index){
            $("#question" + index + " .instructionSlide").hide();
            $("#question" + index + " .sentenceSlide").show();
            var date = new Date();
            self.questions[index].startTime = date.getTime();
        };

        self.submitting = false;
        this.submitResults = function () {
            if(self.submitting == true){
                return;
            }

            self.submitting = true;

            var answer = {};

            for (var i = 0; i < self.questions.length; i++) {
                var question = self.questions[i];
                answer["choice" + (i + 1)] = question.choice;
                answer["answer" + (i + 1)] = question.answer;
                answer["readingTime" + (i + 1)] = question.readingTime;
            }

            var result = {
                experimentType : "RephrasingExperiment",
                questionId: self.questionId,
                workerId: self.workerId,
                age : self.age,
                startedLearning : self.startedLearning,
                answer: answer
            };

            $http.post("/submitResults", result)
                .success(function () {
                    $("#form").submit();
                })
                .error(function () {
                    setTimeout(function () {
                        self.submitting = false;
                        self.submitResults();
                    }, 2000);
                });
        };

        this.questionFinished = function(index){
            $(self.questions[index].order[1]).hide();
            $("#question" + index).hide();
            if(index + 1 < self.questions.length){
                $("#question" + (index + 1)).show();
            }else{
                self.submitResults();
            }
        };

        this.fillerFinished = function(index){
            $("#question" + index + " .fillerSlide").hide();
            $("#question" + index + " .taskSlide").show();
        };

        this.showFillerSentence = function(questionIndex){
            $("#question" + questionIndex + " .fillerInstructionSlide").hide();
            $("#question" + questionIndex + " .fillerSentenceSlide").show();
        };


        this.showFillerQuestion = function(questionIndex){
            $("#question" + questionIndex + " .fillerSentenceSlide").hide();
            $("#question" + questionIndex + " .fillerQuestionSlide").show();
        };

        this.showFillerRephrase = function(questionIndex){
            $("#question" + questionIndex +  " .fillerQuestionSlide").hide();
            $("#question" + questionIndex +  " .fillerRephraseSlide").show();
        };

        this.Question = function (sentence, question, questionFirst,fillerSentence,fillerQuestion) {
            var self = this;
            self.order = [];
            self.sentence = sentence;
            self.question = question;
            self.questionFirst = questionFirst;
            self.fillerSentence = fillerSentence;
            self.fillerQuestion = fillerQuestion;
            this.answer = "";
            this.lastSlide = false;
            this.readingTime = -1;
            this.startTime = -1;

            // this.choice (bool) will be created dynamically while execution !

            if (self.questionFirst){
                self.order = [".questionSlide",".repetitionSlide"];
            }else{
                self.order = [".repetitionSlide",".questionSlide"];
            }

        };

        $(document).ready(function () {
            $(document).on("change",".questionRadio0",function(){
                $(".questionButton0").removeAttr("disabled");
            });
            $(document).on("change",".questionRadio1",function(){
                $(".questionButton1").removeAttr("disabled");
            });

            $(document).on("keydown",".repetitionArea0",function(){
                $(".repetitionButton0").removeAttr("disabled");
            });

            $(document).on("keydown",".repetitionArea1",function(){
                $(".repetitionButton1").removeAttr("disabled");
            });

            $(document).on("change","input[name=fillerRadio0]",function(){
                $("#fillerRadioButton0").removeAttr("disabled");
            });

            $(document).on("change","input[name=fillerRadio1]",function(){
                $("#fillerRadioButton1").removeAttr("disabled");
            });

            $(document).on("keydown","#fillerArea0",function(){
                $(".repetitionFillerButton0").removeAttr("disabled");
            });

            $(document).on("keydown","#fillerArea1",function(){
                $(".repetitionFillerButton1").removeAttr("disabled");
            });

            $("input[type=number]").on("change",function(){
                $("#statisticsButton").removeAttr("disabled");
                if ($(this).val() < 0){
                    $(this).val(0);
                }
                if ($(this).val() > 100){
                    $(this).val(100);
                }
            });

            $(document).on("keypress", ":input:not(textarea)", function(event) {
                if (event.keyCode == 13) {
                    event.preventDefault();
                }
            });

            self.workerId = $("#workerId").val();

            var id = $("#questionId").val();
            self.questionId = id;
            if (id != "") {
                $http.get("/getQuestion/" + id).success(function (data) {
                    var question1 = data.question1;
                    var sentence1 = data.sentence1;
                    var questionFirst1 = data.questionFirst1;
                    var fillerSentence1 = data.fillerSentence1;
                    var fillerQuestion1 = data.fillerQuestion1;

                    self.questions.push(new self.Question(sentence1,question1,questionFirst1,fillerSentence1,fillerQuestion1));

                    var question2 = data.question2;
                    var sentence2 = data.sentence2;
                    var questionFirst2 = data.questionFirst2;
                    var fillerSentence2 = data.fillerSentence2;
                    var fillerQuestion2 = data.fillerQuestion2;

                    self.questions.push(new self.Question(sentence2,question2,questionFirst2,fillerSentence2,fillerQuestion2));
                });

            }
        });

    }]);
})();