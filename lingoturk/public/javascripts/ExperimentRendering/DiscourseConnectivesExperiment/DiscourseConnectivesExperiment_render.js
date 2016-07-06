(function () {
    var app = angular.module('DiscourseConnectivesExperimentApp', []);

    app.controller('RenderController', ['$http', '$timeout', function ($http, $timeout) {
        var self = this;
        this.id = -1;
        this.sentence1 = "";
        this.sentence2 = "";
        this.type = "";

        this.chosenCategories = [];
        this.categorySet = false;
        this.connectives = [];
        this.categories = [];
        this.manualAnswer = "";
        this.noneOfThese = false;
        this.suitableCategory = "";

        /*
         *	Taken from: http://stackoverflow.com/questions/20789373/shuffle-array-in-ng-repeat-angular
         *	-> Fisher–Yates shuffle algorithm
         */
        this.shuffleArray = function (array) {
            var m = array.length, t, i;

            // While there remain elements to shuffle
            while (m) {
                // Pick a remaining element…
                i = Math.floor(Math.random() * m--);

                // And swap it with the current element.
                t = array[m];
                array[m] = array[i];
                array[i] = t;
            }

            return array;
        };

        this.setNoneOfThese = function (value) {
            this.noneOfThese = value;
            this.update();

            if (value == false) {
                this.manualAnswer = "";
                $("#submitButton").attr("disabled", "disabled");
                var scope = angular.element($("#angularApp")).scope();
                $timeout(function () {
                    scope.$apply();
                    self.initCategorySortable();
                });
            } else {
                $("#submitButton").removeAttr("disabled");
            }

        };

        this.init = function () {
            var id = $("#questionId").val();
            if (id != "") {
                $http.get("/getQuestion/" + id).success(function (data) {
                    self.id = data.id;
                    self.sentence1 = data.sentence1;
                    self.sentence2 = data.sentence2;
                    self.type = data.type;
                });
                $http.get("/getConnectives/" + id).success(function (data) {
                    self.categories = self.shuffleArray(data.categories);
                });
            }
        };

        this.update = function () {
            var scope = angular.element($("#angularApp")).scope();
            $timeout(function () {
                scope.$apply();
            });
        };

        this.collision = function ($div1, $div2) {
            var x1 = $div1.offset().left;
            var y1 = $div1.offset().top;
            var h1 = $div1.outerHeight(true);
            var w1 = $div1.outerWidth(true);
            var b1 = y1 + h1;
            var r1 = x1 + w1;
            var x2 = $div2.offset().left;
            var y2 = $div2.offset().top;
            var h2 = $div2.outerHeight(true);
            var w2 = $div2.outerWidth(true);
            var b2 = y2 + h2;
            var r2 = x2 + w2;

            if (b1 < y2 || y1 > b2 || r1 < x2 || x1 > r2) return false;
            return true;
        };

        this.testCollision = function(item,list){
            var found = false;
            list.each(function(){
                if(self.collision(item,$(this))){
                    found = true;
                    return true;
                }
            });
            if(found){
                return true;
            }
            return false;
        };

        this.removeSuitableCategory = function(suitableCategory){
            var index = self.chosenCategories.indexOf(suitableCategory);
            if(index > -1){
                self.chosenCategories.splice(index,1);
                self.updateConnectives();
            }
            if(self.chosenCategories.length == 0){
                self.categoryIsSet(false);
            }
        };

        this.updateConnectives = function(){
            var tmp = [];
            for(var i = 0; i < self.categories.length; i++){
                if($.inArray(self.categories[i].category,self.suitableCategories) != -1){
                    tmp = tmp.concat(self.categories[i].connectives);
                }
            }

            self.connectives = tmp;
            $("#sortableBody").css('background-color', '#ffe7c7');
            self.update();
        };

        this.categoriesChosen = function () {
            self.suitableCategories = self.chosenCategories;
            self.updateConnectives();

            $("#sortableBody").css('background-color', '#ffe7c7');
            self.categoryIsSet(true);
        };

        this.init();

        this.update();

        this.addValidConnectivesCategory = function(){
            var index = $("#validConnectivesContainer > div").length + 1;
            var code = $("<div> <div class='panel panel-warning' > <div class='panel-heading' style='color:#000000'>valid connective</div> <div class='panel-body' style='background-color: #DFFFBF;'> <ul style='margin-bottom:0px' class='list-inline sortable sortableValidConnectives' id='sortableValidConnectives_" + index + "' > </ul> </div> </div> </div>");
            $("#validConnectivesContainer > div").last().after(code);
            self.update();
            self.initConnectivesSortable();
        };

        this.initConnectivesSortable = function () {
            $(".sortable").sortable({
                connectWith: ".sortable",
                items: ".draggable",
                revert: true,
                sort: function (event, ui) {
                    if (self.collision(ui.item, $("#sortableConnectives")) || self.collision(ui.item, $("#sortableNotRelevant")) || self.collision(ui.item, $("#sortableCantDecide")) || self.testCollision(ui.item, $(".sortableValidConnectives"))) {
                        ui.item.removeClass("no-drop");
                        ui.item.addClass("allow-drop");
                    } else {
                        ui.item.removeClass("allow-drop");
                        ui.item.addClass("no-drop");
                    }
                },
                receive: function (event, ui) {
                    var connective = $(ui.item).text().trim();
                    if ($("#sortableConnectives").find("li").length == 0) {
                        $("#submitButton").removeAttr("disabled");
                    } else {
                        $("#submitButton").attr("disabled", "disabled");
                    }
                },
                start: function () {
                    $("#sortableDiv").css("border-style", "groove");
                },
                stop: function () {
                    $("#sortableDiv").css("border-style", "hidden");
                }
            });
        };

        this.initCategorySortable = function () {
            $("#category,#suitableCategory").sortable({
                connectWith: ".sortable",
                items: ".draggable",
                revert: true,
                sort: function (event, ui) {
                    ui.item.removeClass("normal");
                    ui.item.removeClass("pointer");
                    if (self.collision(ui.item, $("#category")) || self.collision(ui.item, $("#suitableCategory"))) {
                        ui.item.removeClass("no-drop");
                        ui.item.addClass("allow-drop");
                    } else {
                        ui.item.removeClass("allow-drop");
                        ui.item.addClass("no-drop");
                    }
                },
                stop: function (event, ui) {
                    ui.item.removeClass("allow-drop");
                    ui.item.removeClass("no-drop");
                },
                receive: function (event, ui) {
                    var connective = $(ui.item).text().trim();
                    self.chosenCategories.push(connective);
                    $("#categoriesChosenButton").removeAttr("disabled");
                },
                start: function () {
                    $("#sortableDiv").css("border-style", "groove");
                },
                stop: function () {
                    $("#sortableDiv").css("border-style", "hidden");
                }
            });
        };

        this.categoryIsSet = function (value) {
            this.categorySet = value;
            if (value == true) {
                $("#sortableBody").css('background-color', '#FFD18D');
                var scope = angular.element($("#angularApp")).scope();
                $timeout(function () {
                    scope.$apply();
                    self.initConnectivesSortable();
                });
            } else {
                $("#sortableBody").css('background-color', '#FFFFFF');
                $("#submitButton").attr("disabled", "disabled");
                this.suitableCategory = "";
                this.manualAnswer = "";
            }
            this.update();

            var scope = angular.element($("#angularApp")).scope();
            $timeout(function () {
                scope.$apply();
                self.initCategorySortable();
            });
        };

        this.finished = function () {
            var notRelevant = "";
            $("#sortableNotRelevant").find("li").each(function(index,element){
                if (index == $("#sortableNotRelevant").find("li").length - 1) {
                    notRelevant = notRelevant + $(this).text().trim();
                }else {
                    notRelevant = notRelevant + $(this).text().trim() + ", ";
                }
            });
            $("#notRelevant").val(notRelevant);

            var cantDecide = "";
            $("#sortableCantDecide").find("li").each(function(index,element){
                if (index == $("#sortableCantDecide").find("li").length - 1) {
                    cantDecide = cantDecide + $(this).text().trim();
                }else {
                    cantDecide = cantDecide + $(this).text().trim() + ", ";
                }
            });
            $("#cantDecide").val(cantDecide);

            $(".sortableValidConnectives").each(function(){
                var id = $(this).attr("id").split("_")[1];
                var validConnectives = "";

                if($(this).find("li").length == 0){
                    return;
                }

                $(this).find("li").each(function(index,element){
                    if (index == $("#sortableCantDecide").find("li").length - 1) {
                        validConnectives = validConnectives + $(this).text().trim();
                    }else {
                        validConnectives = validConnectives + $(this).text().trim() + ", ";
                    }
                });
                var attribute = "validConnectives_" + id;
                var code = $("<input type='hidden' name='" + attribute + "' id='" + attribute + "' value='" + validConnectives + "' >");
                $("#form").append(code);
            });

            $("#manualAnswer").val(self.manualAnswer);
            $("#category").val(self.suitableCategory);
            if (self.type == "CD_Q") {
                var assignment_ID = $("#assignmentId").val();
                $http.post("/submitResult", {assignmentID: assignment_ID}).success(function () {
                    $("#form").submit();
                }).error(function () {
                    $("#form").submit();
                });
            } else {
                $("#form").submit();
            }
        };

        $(document).ready(function () {
            self.initCategorySortable();

            $(document).on("mouseenter", ".draggable", function () {
                $(this).addClass("pointer");
                $(this).removeClass("normal");
            });
            $(document).on("mouseleave", ".draggable", function () {
                $(this).addClass("normal");
                $(this).removeClass("pointer");
                $(this).removeClass("allow-drop");
                $(this).removeClass("no-drop");
            });

            $(document).on("keypress", ":input:not(textarea)", function(event) {
                if (event.keyCode == 13) {
                    event.preventDefault();
                }
            });
        });
    }
    ])
    ;
})();