(function () {
    var app = angular.module('ExperimentCreationInterface', []);

    app.controller("InterfaceController", ["$http", function ($http) {
        var self = this;

        this.newTypeName = "";
        this.experimentType = "REUSE";
        this.sourceTypeName = "";
        this.sourceGroupName = "FullGroup";
        this.sourceListType = "MULTIPLE LISTS";
        this.types = [{name: "String (can also be an image path)", val: "String"}, {name: "Number (Integer)", val: "int"}, {
            name: "Number (Float)",
            val: "float"
        }];

        this.questionFields = [{name: "", type: ""}];

        this.Field = function () {
            var self = this;
            self.name = "";
            self.type = "";
        };

        this.addField = function () {
            self.questionFields.push(new self.Field());
        };

        this.removeField = function (field) {
            var index = self.questionFields.indexOf(field);
            self.questionFields.splice(index, 1);
        };

        this.isAlphaNumeric = function (s) {
            return /^[a-z][a-z0-9]*$/i.test(s);
        };

        this.checkInput = function () {
            for (var i = 0; i < self.questionFields.length; ++i) {
                var f = self.questionFields[i];
                if (f.name == "" || !self.isAlphaNumeric(f.name.trim()) || f.type == "") {
                    return false;
                }
            }
            return true;

        };

        this.submitting = false;
        this.submit = function () {
            if (self.newTypeName.trim() != "" && self.isAlphaNumeric(self.newTypeName.trim())) {
                if (!self.submitting) {
                    self.submitting = true;
                    var result = {
                        newTypeName: self.newTypeName,
                        experimentType: self.experimentType,
                        sourceTypeName: self.sourceTypeName,
                        sourceGroupName: self.sourceGroupName,
                        questionFields: self.questionFields,
                        sourceListType: self.sourceListType
                    };
                    $http.post("/createNewExperimentType", result)
                        .success(function () {
                            bootbox.alert("Experiment type created. You will be redirected to the index page!", function () {
                                window.location.href = "/";
                            });
                        })
                        .error(function (data) {
                            bootbox.alert("An error occured while creating your experiment: " + data);
                            self.submitting = false;
                        });
                }
            } else {
                bootbox.alert("Error! New type name is either empty or not alphanumeric.");
            }
        };

        $(document).ready(function(){
            // http://stackoverflow.com/questions/20870671/bootstrap-3-btn-group-lose-active-class-when-click-any-where-on-the-page
            $(".btn-group > .btn").click(function(){
                $(this).addClass("active").siblings().removeClass("active");
            });
        });

    }]);
})();