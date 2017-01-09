(function () {
    var app = angular.module('ExtractionInterface', []);

    app.controller('ExtractionController', ['$http', '$timeout', function ($http, $timeout) {
        var self = this;
        self.expId = null;
        self.experimentType = null;
        self.useStored = false;
        self.query = "";
        self.allFields = [];
        self.useField = [];
        self.orderBy = ["partId", "questionId", "workerId"];
        self.usedFields = [];
        self.buildQuery = function(){
            var usedFields = [];

            $("#fieldTable").find("tbody").find(".field").each(function(index,elem){
                var index = parseInt($(elem).attr("id").replace("field_",""));
                if(self.useField[index]){
                    usedFields.push(self.allFields[index]);
                }
            });

            self.usedFields = usedFields;

            self.query = "SELECT " + (usedFields.length > 0 ? usedFields.join(", ") : "*") + " FROM (\n\t" + "SELECT * FROM " + self.experimentType + "Results\n\tLEFT OUTER JOIN Questions USING (QuestionId)\n\tLEFT OUTER JOIN Groups USING (PartId)\n) as tmp\nWHERE LingoExpModelId = " + self.expId;
            if(self.orderBy.length > 0){
                self.query += "\nORDER BY " + self.orderBy.join(", ");
            }
            var scope = angular.element($("#angularApp")).scope();
            $timeout(function () {
                scope.$apply();
            });

            return usedFields;
        };

        self.executeQuery = function(){
            if(self.query == ""){
                self.buildQuery();
            }
            var d = {
                expId : self.expId,
                experimentType : self.experimentType,
                useStored : self.useStored,
                usedFields : self.usedFields,
                allFields : self.allFields,
                orderBy : self.orderBy
            };
            return "/loadResults?d=" + encodeURIComponent(JSON.stringify(d));
        };

        $(document).ready(function(){
            self.expId = parseInt($("#expId").val());
            self.experimentType = $("#experimentType").val();

            $http.get("/getExperimentDetails?experimentName=" + self.experimentType).success(function (data) {
                self.types = data;
                for (var typeName in data) {
                    if (!data.hasOwnProperty(typeName)) continue;
                    var obj = data[typeName];
                    for(var field in obj.fields){
                        if (!obj.fields.hasOwnProperty(field)) continue;

                        if (obj.isQuestionType) {
                            self.allFields.push(self.experimentType + "_" + obj.fields[field].name);
                        }else{
                            self.allFields.push(obj.fields[field].name);
                        }
                    }
                }
                self.useField = new Array(self.allFields.length).fill(true);

                self.buildQuery();

                $("#fieldTable").find("tbody").sortable({
                    stop : function(){
                        self.buildQuery();
                    }
                });
            });
        })
    }]);
})();