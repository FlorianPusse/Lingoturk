(function () {
    var app = angular.module('ExperimentCreationInterface', []);

    app.controller("InterfaceController", ["$http", function ($http) {
        var self = this;

        this.newTypeName = "";
        this.experimentType = "COPY";
        this.sourceTypeName = "";
        this.sourceGroupName = "";
        this.types =  [{name: "String", val: "String"},{name: "Integer", val: "int"},{name: "Float", val: "float"}];

        this.reuseCheckbox = "NEW";

        this.questionFields = [{name : "", type : ""}];

        this.Field = function(){
            var self = this;
            self.name = "";
            self.type = "";
        };

        this.addField = function(){
            self.questionFields.push(new self.Field());
        };

        this.submitting = false;
        this.submit = function () {
            if (!self.submitting) {
                self.submitting = true;
                var result = {
                    newTypeName : self.newTypeName,
                    experimentType : self.experimentType,
                    sourceTypeName : self.sourceTypeName,
                    sourceGroupName : self.sourceGroupName,
                    questionFields : self.questionFields
                };
                $http.post("/createNewExperimentType", result)
                    .success(function () {
                        bootbox.alert("Experiment type created. You will be redirected to the index page!", function() {
                            window.location.href = "/";
                        });
                    })
                    .error(function (data) {
                        alert(data);
                        self.submitting = false;
                    });
            }
        };
    }]);
})();