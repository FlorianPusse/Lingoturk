(function () {
    var app = angular.module('ExperimentCreationInterface', []);

    app.controller("InterfaceController", ["$http", function ($http) {
        var self = this;

        this.newTypeName = "";
        this.experimentType = "REUSE";
        this.sourceTypeName = "";
        this.sourceGroupName = "FullGroup";
        this.types =  [{name: "String (can also be an image path)", val: "String"},{name: "Number (Integer)", val: "int"},{name: "Number (Float)", val: "float"}];

        this.questionFields = [{name : "", type : ""}];

        this.Field = function(){
            var self = this;
            self.name = "";
            self.type = "";
        };

        this.addField = function(){
            self.questionFields.push(new self.Field());
        };

        this.removeField = function(field){
            var index = self.questionFields.indexOf(field);
            self.questionFields.splice(index,1);
        };

        this.checkInput = function(){
            for(var i = 0; i < self.questionFields.length; ++i){
                var f = self.questionFields[i];
                if(f.name == "" || f.type == ""){
                    return false;
                }
            }
            return true;
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