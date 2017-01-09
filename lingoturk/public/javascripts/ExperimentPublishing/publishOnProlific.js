(function () {
    var app = angular.module('PublishProlific', ["Lingoturk"]);

    app.controller('PublishingController', ['$http', '$timeout', '$scope', function ($http, $timeout, $scope) {
        var self = this;
        self.defaultValue = 0;
        self.experimentURL = "";
        self.updates = {};
        self.expId = -1;
        self.lifetime = -1;
        self.useAdvancedMode = false;
        self.maxWorkingTime = 0;

        self.submit = function () {
            var data = {
                type: self.type,
                updates: self.updates,
                lifetime: self.lifetime,
                expId: self.expId,
                maxWorkingTime : self.maxWorkingTime,
                useAdvancedMode : self.useAdvancedMode,
                defaultValue: self.defaultValue
            };

            $http.post("/publishProlific", data)
                .success(function () {
                    $("#submitButton").hide();
                    $("#successButton").show();
                })
                .error(function (data) {
                    $("#submitButton").hide();
                    $("#failButton").show();
                    $("#message").text("Message: " + data);
                });
        };

        $(document).ready(function () {
            $("#failButton").hide();
            $("#successButton").hide();
            $("#tabs").tabs({});

            $("#completionUrl").on("input", function(){
                $("#experimentUrl").text(encodeURIComponent(this.value));
            });

            $("#useAdvancedMode").change(function () {
                if (this.checked) {
                    self.useAdvancedMode = true;
                    bootbox.confirm("Apply given \"participants per list\" to each list?", function (result) {
                        // either apply self.defaultValue to each list or ignore values
                        $("#advancedModeBox").show();
                        if (result && self.defaultValue != null) {
                            $("input[name='individualNumber']").val(self.defaultValue);
                            for (var v in self.updates) {
                                if (self.updates.hasOwnProperty(v)) {
                                    self.updates[v]["maxParticipants"] = self.defaultValue;
                                }
                            }
                        }
                    });
                } else {
                    bootbox.confirm("Discard changes applied in advanced mode?", function (result) {
                        if (result) {
                            $("#advancedModeBox").hide();
                            // enable all, use self.defaultValue
                            for (var v in self.updates) {
                                if (self.updates.hasOwnProperty(v)) {
                                    self.updates[v]["disabled"] = false;
                                    self.updates[v]["maxParticipants"] = self.defaultValue;
                                }
                            }
                            self.useAdvancedMode = false;
                        } else {
                            $('#useAdvancedMode')[0].checked = true;
                            self.useAdvancedMode = true;
                        }
                        $timeout(function () {
                            angular.element($("#angularApp")).scope().$apply();
                        });
                    });
                }
                $timeout(function () {
                    angular.element($("#angularApp")).scope().$apply();
                });
            });
        });
    }]);
})();