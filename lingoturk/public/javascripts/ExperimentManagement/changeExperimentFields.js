(function () {
    var app = angular.module('CreateExperiment', ["Lingoturk"]);

    app.controller('CreationController', ['$http', '$timeout', '$scope', function ($http, $timeout, $scope) {
        var self = this;
        self.types = [];
        self.activeClass = "";

        this.addField = function(c){
            c.fields.push({name : "", type : ""});
        };

        this.removeField = function(c,field){
            var index = c.fields.indexOf(field);
            c.fields.splice(index,1);
        };

        this.addClass = function(){
            bootbox.prompt("Enter new classname", function(result) {
                if (result === null) {
                    return;
                } else {
                    $scope.$apply(self.types[result] = {fields: [{name : "", type : ""}], path: ""});
                }
            });
        };

        this.submit = function(){
            var data = { type : self.type, types : self.types};
            $http.post("/submitNewFields", data)
                .success(function () {
                    bootbox.alert("Fields changed. You will be redirected to the index page!", function() {
                        window.location.href = "/";
                    });
                })
                .error(function () {
                    bootbox.alert("Could not change experiment field.", function() {
                    });
                    self.submitting = false;
                });
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
                    if (!data.hasOwnProperty(typeName)){
                        continue;
                    }
                    var obj = data[typeName];
                    if (obj.isGroupType) {
                        delete data[typeName];
                        continue;
                    }
                    if(self.activeClass == ""){
                        self.activeClass = typeName;
                    }
                }
            });
        });
    }]);
})();