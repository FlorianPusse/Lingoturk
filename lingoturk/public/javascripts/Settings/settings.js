(function () {
    var app = angular.module('Settings', []);

    app.controller('SettingsController', ['$http', '$timeout', function ($http, $timeout) {
        var self = this;
        this.ipAddress = "";
        this.accessKey = "";
        this.secretKey = "";
        this.oldPassword = "";
        this.newPassword1 = "";
        this.newPassword2 = "";

        this.isDirty = function(){
            if(self.ipAddress != "" || self.accessKey != "" || self.secretKey != "" || self.oldPassword != ""
                || self.newPassword1 != "" || self.newPassword2 != ""){
                return true;
            }
            return false;
        };

        this.submit = function (answer) {
            if (!answer) {
                return;
            }

            var data = {
                ipAddress : self.ipAddress,
                accessKey : self.accessKey,
                secretKey : self.secretKey,
                oldPassword : self.oldPassword,
                newPassword1 : self.newPassword1,
                newPassword2 : self.newPassword2
            };

            $http.post("/globalSettingsChanged", data)
                .success(function () {
                    $("#submitButton").hide();
                    $("#successButton").show();
                    $timeout(function(){
                        self.displayNormalButton();
                    },7000);
                })
                .error(function (data) {
                    $("#submitButton").hide();
                    $("#failButton").show();
                    $("#message").text("Message: " + data);
                    $timeout(function(){
                        self.displayNormalButton();
                    },7000);
                });
        };

        this.displayNormalButton = function(){
            $("#failButton").hide();
            $("#successButton").hide();
            $("#submitButton").show();
        };

        this.submitButtonClick = function () {
            bootbox.confirm({
                message: "Applying changes will overwrite your settings!",
                title: "Warning!",
                callback: self.submit
            });
        };

        $(document).ready(function () {
            $(".settingsContainer").first().show();

            $("button[data-optionType]").click(function () {
                $(this).addClass("active").siblings().removeClass("active");
                var containerObject = $($(this).attr("data-optionType"));
                containerObject.show();
                containerObject.siblings(".settingsContainer").hide();
            });

            $("input[type='text']").on("keydown",function(event){
                if(event.which == 9 || event.which == 32){
                    return false;
                }
            });
        })

    }]);
})();
