(function () {
    var app = angular.module('CreateExperiment', ["Lingoturk"]);

    app.controller('CreationController', ['$http', '$timeout', '$scope', function ($http, $timeout, $scope) {
        $scope.window = window;
        $scope.alert = alert;
        $scope.stringify = JSON.stringify;

        var self = this;
        this.id = -1;
        this.types = [];
        this.type = "";
        this.name = "";
        this.nameOnAmt = "";
        this.description = "";
        this.additionalExplanations = "";
        this.questionType = null;
        this.groupType = null;

        this.exampleQuestions = [];
        this.groups = [];
        this.files = [];

        this.delimiters = [
            {name: ',', val : ','},
            {name: ';', val : ';'},
            {name: 'tab', val : '\t'},
            {name: 'space', val : ' '}
        ];
        this.delimiter = ',';
        this.commentSequence = '#';

        self.questionColumnNames = null;

        $scope.range = function (max) {
            var arr = [];
            for (var i = 0; i < max; ++i) {
                arr.push(i);
            }
            return arr;
        };

        this.submit = function(){
            for(var i = 0; i < self.groups.length; ++i){
                var group = self.groups[i];
                var questions = [];
                group.questions = questions;
                var content = group.parsedContent;
                var columnNames = self.questionColumnNames;

                for(var j = 0; j < content.length; ++j){
                    var question = self.createQuestion();
                    for(var c = 0; c < columnNames.length; ++c){
                        if(columnNames[c].trim() == "" || columnNames[c].trim() == "-- select field --"){
                            continue;
                        }
                        question[columnNames[c]] = content[j][c];
                    }
                    questions.push(question);
                }
            }

            var experiment = {
                id: this.id,
                name: this.name,
                nameOnAmt: this.nameOnAmt,
                description: this.description,
                additionalExplanations: this.additionalExplanations,
                type : this.type + "Experiment",
                exampleQuestions: this.exampleQuestions,
                parts: this.groups
            };

            $http.post("/submitNew_Experiment", experiment)
                .success(function () {
                    bootbox.alert("Experiment created. You will be redirected to the index page!", function() {
                        window.location.href = "/";
                    });
                })
                .error(function () {
                    alert("Error!");
                });
        };

        this.createGroup = function () {
            return eval("new self." + self.groupType.name + "()");
        };

        this.createQuestion = function(){
            return eval("new self." + self.questionType.name + "()");
        };

        this.addGroup = function (group) {
            self.groups.push((group === undefined) ? self.createGroup() : group);
            $timeout(function () {
                angular.element($("#angularApp")).scope().$apply();
                $("#allquestions").accordion("refresh");
                $("#allquestions").accordion("option", "active", -1);
            });
        };

        this.resetGroups = function () {
            self.groups = [];
            $timeout(function () {
                angular.element($("#angularApp")).scope().$apply()
            });
            self.questionColumnNames = null;
            self.usedNames = [];
            self.files = [];
            $("input[type=file]").val("");
        };

        this.newFieldSelected = function (newValue, oldValue) {
            if (newValue != "" && newValue.trim() != "-- select field --") {
                self.usedNames.push(newValue);
            }
            if (oldValue != "") {
                var index = self.usedNames.indexOf(oldValue);
                if(index != -1){
                    self.usedNames.splice(index, 1);
                }
            }
        };

        this.delimiterChanged = function(){
            self.groups = [];
            self.questionColumnNames = null;
            for(var i = 0; i < self.files.length; ++i){
                self.processFile(self.files[i]);
            }
            $timeout(function () {
                angular.element($("#angularApp")).scope().$apply()
            });
        };

        this.fileLoaded = function (fileObject) {
            self.files.push(fileObject);
            self.processFile(fileObject);
        };

        this.processFile = function(fileObject){
            var parsedContent = CSVToArray(fileObject.fileContent, self.delimiter);

            var columnNames = new Array(parsedContent[0].length);
            for (var i = 0; i < columnNames.length; ++i) {
                columnNames[i] = "";
            }
            if(self.questionColumnNames == null){
                self.questionColumnNames = columnNames;
            }

            for (var i = 0; i < parsedContent.length; ++i) {
                row = parsedContent[i];
                var notAllEmpty = false;
                for (var j = 0; j < row.length; ++j) {
                    if (row[j] !== undefined && row[j].trim() != "") {
                        notAllEmpty = true;
                    }
                }

                if (!notAllEmpty || row[0].startsWith(self.commentSequence)) {
                    parsedContent.splice(i, 1);
                }
            }

            var group = self.createGroup();
            group.fileName = fileObject.fileName;
            group.parsedContent = parsedContent;

            self.addGroup(group);
        };

        $(document).ready(function () {
            if (typeof String.prototype.startsWith != 'function') {
                String.prototype.startsWith = function (str) {
                    return this.slice(0, str.length) == str;
                };
            }

            self.type = $("#experimentName").val().trim();
            $http.get("/getExperimentDetails?experimentName=" + self.type).success(function (data) {
                self.types = data;
                for (var typeName in data) {
                    if (!data.hasOwnProperty(typeName)) continue;
                    var obj = data[typeName];
                    var fields = obj.fields;

                    if (obj.isQuestionType) {
                        obj.name = typeName;
                        self.questionType = obj;
                    }

                    if (obj.isGroupType) {
                        obj.name = typeName;
                        self.groupType = obj;
                    }

                    var tmp = [];
                    for (var i = 0; i < fields.length; ++i) {
                        var f = fields[i];
                        tmp.push(f.name);
                    }
                    var func = "self[typeName] = function(" + tmp.join(",") + "){\nvar self = this;\nself._type=\"" + typeName + "\";\n"
                    if(obj.isGroupType){
                        func + "self.questions=null;\n";
                    }
                    for (var i = 0; i < fields.length; ++i) {
                        var f = fields[i];
                        func += "self." + f.name + " = " + f.name + ";\n"
                    }
                    func += "};";
                    eval(func);
                }

                self.questionFieldNames = self.questionType.fields;
                self.usedNames = [];

                // Create Tabs
                $("#tabs").tabs({
                    //heightStyle: "content"
                });

                $("#allquestions").accordion({
                    heightStyle: "content"
                });

            });
        })
    }
    ]);
})();