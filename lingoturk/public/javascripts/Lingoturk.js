(function () {
    var app = angular.module('Lingoturk', []);

    app.directive("statisticsSlide", function($compile){
        return {
            restrict: 'E',
            templateUrl: '/assets/templates/statisticsDirective.html',
            scope: {
                content : '='
            },
            compile: function(tElement, tAttr) {
                var contents = tElement.contents().remove();
                var compiledContents;
                return function(scope, iElement, iAttr) {
                    if(!compiledContents) {
                        compiledContents = $compile(contents);
                    }
                    compiledContents(scope, function(clone, scope) {
                        iElement.append(clone);
                    });
                };
            }
        };
    });

    app.directive("statisticsSlide", function($compile){
        return {
            restrict: 'E',
            templateUrl: '/assets/templates/workeridDirective.html',
            scope: {
                content : '='
            },
            compile: function(tElement, tAttr) {
                var contents = tElement.contents().remove();
                var compiledContents;
                return function(scope, iElement, iAttr) {
                    if(!compiledContents) {
                        compiledContents = $compile(contents);
                    }
                    compiledContents(scope, function(clone, scope) {
                        iElement.append(clone);
                    });
                };
            }
        };
    });

    app.directive("slide", function($compile){
        return {
            restrict: 'E',
            templateUrl: '/assets/templates/slide.html',
            scope: {
                content : '='
            },
            compile: function(tElement, tAttr) {
                var contents = tElement.contents().remove();
                var compiledContents;
                return function(scope, iElement, iAttr) {
                    if(!compiledContents) {
                        compiledContents = $compile(contents);
                    }
                    compiledContents(scope, function(clone, scope) {
                        iElement.append(clone);
                    });
                };
            }
        };
    });

    app.directive("instructions", function($compile){
        return {
            restrict: 'E',
            templateUrl: '/assets/templates/slide.html',
            scope: {
                content : '='
            },
            compile: function(tElement, tAttr) {
                var contents = tElement.contents().remove();
                var compiledContents;
                return function(scope, iElement, iAttr) {
                    if(!compiledContents) {
                        compiledContents = $compile(contents);
                    }
                    compiledContents(scope, function(clone, scope) {
                        iElement.append(clone);
                    });
                };
            }
        };
    });

    app.directive("textAnswerPanel", function(){
        return {
            restrict: 'E',
            templateUrl: '/assets/templates/textAnswerPanel.html',
            scope: {
                content : '=',
                answer : '=',
                click : '=',
                restrictAnswer : '@?'
            },
            link: function (scope, element, attrs) {
                $(element).find("input").on("input",function(){
                    scope.matches = new RegExp(attrs.restrictAnswer).test(scope.answer);
                })
            }
        };
    });

    app.directive("starAnswerPanel", function(){
        return {
            restrict: 'E',
            templateUrl: '/assets/templates/starAnswerPanel.html',
            scope: {
                content : '=',
                answer : '=',
                click : '=',
                maxStars : '=?'
            },
            link: function (scope, element, attrs) {
                if(scope.maxStars === undefined){
                    scope.maxStars = 5;
                }

                scope.range = function(max){
                    var result = [];
                    for(var i = 0; i < max; ++i){
                        result.push(i);
                    }
                    return result;
                };

                scope.setAnswer = function(i){
                    scope.answer = i;
                };
            }
        };
    });

    app.directive("dragAndDrop", ['$timeout',function($timeout){
        return {
            restrict: 'E',
            templateUrl: '/assets/templates/dragAndDrop.html',
            scope: {
                content : '=',
                answer : '=',
                click : '='
            },
            link: function (scope, element, attrs) {
                scope.connectives = ["because","as a result", "as an illustration", "more specifically", "in addition", "even though", "nevertheless", "by contrast"];
                scope.chosenConnectives = [];
                scope.suitableCategories = [];
                scope.manualAnswer = "";
                scope.manualAnswer2 = "";
                scope.noneOfThese = false;
                scope.noneOfThese2 = false;
                scope.suitableCategory = "";
                scope.categorySet = false;

                scope.setNoneOfThese = function (value) {
                    scope.noneOfThese = value;
                    scope.update();

                    if (value == false) {
                        scope.manualAnswer = "";
                        $("#submitButton").attr("disabled", "disabled");
                        $timeout(function () {
                            scope.$apply();
                            scope.initCategorySortable();
                        });
                    } else {
                        $("#submitButton").removeAttr("disabled");
                    }
                };

                scope.setNoneOfThese2 = function (value) {
                    scope.noneOfThese2 = value;
                    scope.update();

                    if (value == false) {
                        scope.manualAnswer2 = "";
                        $("#submitButton").attr("disabled", "disabled");
                        $timeout(function () {
                            scope.$apply();
                            scope.initConnectivesSortable();
                        });
                    } else {
                        $("#submitButton").removeAttr("disabled");
                    }

                };

                scope.update = function () {
                    $timeout(function () {
                        scope.$apply();
                    });
                };

                scope.collision = function ($div1, $div2) {
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

                scope.testCollision = function(item,list){
                    var found = false;
                    list.each(function(){
                        if(scope.collision(item,$(this))){
                            found = true;
                            return true;
                        }
                    });
                    if(found){
                        return true;
                    }
                    return false;
                };

                scope.setAnswers = function(){
                    if(scope.suitableCategories.length > 0){
                        scope.answer.connective1 = scope.suitableCategories[0];
                        if(scope.chosenConnectives.length > 0){
                            scope.answer.connective2 = scope.chosenConnectives[0];
                        }
                    }else {
                        if (scope.chosenConnectives.length > 0) {
                            scope.answer.connective1 = scope.chosenConnectives[0];
                        }
                    }
                };

                scope.categoriesChosen = function () {
                    scope.suitableCategories = scope.chosenConnectives;
                    scope.chosenConnectives = [];
                    $("#sortableConnectives").parent().removeClass("disabledSortable");
                    $("#sortableBody").removeClass("disabledSortable");
                    $("#sortableBody").css('background-color', '#ffe7c7');
                    scope.categoryIsSet(true);
                };

                scope.initConnectivesSortable = function () {
                    $(".sortable").sortable({
                        connectWith: ".sortable",
                        items: ".draggable",
                        revert: true,
                        sort: function (event, ui) {
                            if (scope.collision(ui.item, $("#sortableConnectives")) || scope.testCollision(ui.item, $("#secondPhase"))) {
                                ui.item.removeClass("no-drop");
                                ui.item.addClass("allow-drop");
                            } else {
                                ui.item.removeClass("allow-drop");
                                ui.item.addClass("no-drop");
                            }
                        },
                        receive: function (event, ui) {
                            var connective = $(ui.item).text().trim();
                            if($(event.target).attr("id") == "secondPhase"){
                                scope.chosenConnectives.push(connective);
                            }else{
                                scope.chosenConnectives.splice(scope.chosenConnectives.indexOf(connective),1);
                            }
                            if(scope.chosenConnectives.length == 1){
                                $("#sortableConnectives").css("pointer-events","none");
                                $("#sortableConnectives").parent().addClass("disabledSortable");
                            }else{
                                $("#sortableConnectives").css("pointer-events","all");
                                $("#sortableConnectives").parent().removeClass("disabledSortable");
                            }
                        }
                    });
                };

                scope.initCategorySortable = function () {
                    $("#category,#suitableCategory").sortable({
                        connectWith: ".sortable",
                        items: ".draggable",
                        revert: true,
                        sort: function (event, ui) {
                            ui.item.removeClass("normal");
                            ui.item.removeClass("pointer");
                            if (scope.collision(ui.item, $("#category")) || scope.collision(ui.item, $("#suitableCategory"))) {
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
                            if($(event.target).attr("id") == "suitableCategory"){
                                scope.chosenConnectives.push(connective);
                                $(".continue-button").removeAttr("disabled");
                            }else{
                                scope.chosenConnectives.splice(scope.chosenConnectives.indexOf(connective),1);
                                $(".continue-button").attr("disabled","disabled");
                            }
                            if(scope.chosenConnectives.length == 1){
                                $("#category").css("pointer-events","none");
                                $("#category").parent().addClass("disabledSortable");
                            }else{
                                $("#category").css("pointer-events","all");
                                $("#category").parent().removeClass("disabledSortable");
                            }
                        }
                    });

                    $(element[0]).on("mouseenter", ".draggable", function () {
                        $(this).addClass("pointer");
                        $(this).removeClass("normal");
                    });
                    $(element[0]).on("mouseleave", ".draggable", function () {
                        $(this).addClass("normal");
                        $(this).removeClass("pointer");
                        $(this).removeClass("allow-drop");
                        $(this).removeClass("no-drop");
                    });
                };

                scope.categoryIsSet = function (value) {
                    scope.categorySet = value;
                    if (value == true) {
                        $("#sortableBody").css('background-color', '#FFD18D');
                        $timeout(function () {
                            scope.$apply();
                            scope.initConnectivesSortable();
                        });
                    } else {
                        $("#sortableBody").css('background-color', '#FFFFFF');
                        $("#submitButton").attr("disabled", "disabled");
                        scope.suitableCategory = "";
                        scope.manualAnswer = "";
                    }
                    scope.update();

                    $timeout(function () {
                        scope.$apply();
                        scope.initCategorySortable();
                    });
                };

                $timeout(scope.initCategorySortable,1000);
            }
        };
    }]);

    app.directive("sliderAnswerPanel", function(){
        return {
            restrict: 'E',
            templateUrl: '/assets/templates/sliderAnswerPanel.html',
            scope: {
                content : '=',
                answer : '=',
                click : '='
            },
            link: function (scope, element, attrs) {
                scope.activated = false;

                var elem = $(element[0]).find(".slider");
                elem.slider({
                    min : 0,
                    max : 100,
                    change: function(event,ui){
                        scope.$apply(scope.answer = ui.value);
                        scope.$apply(scope.activated = true);
                        elem.find(".ui-slider-handle").show();
                    }
                })
                .each(function() {
                    /* http://stackoverflow.com/questions/10224856/jquery-ui-slider-labels-under-slider */
                    var opt = elem.slider("option");
                    var vals = opt.max - opt.min;

                    for (var i = 0; i <= vals; i+= 10) {
                        var el = $('<label style="position: absolute; width: 20px; margin-top: 20px; margin-left: -10px;text-align: center;">' + (i + opt.min) + '%</label>').css('left', (i/vals*100) + '%');
                        elem.append(el);
                    }
                });
                if(scope.content.hideHandle){
                    elem.find(".ui-slider-handle").hide();
                }
            }
        };
    });


    app.directive("fileInput", function(){
        return {
            restrict: 'E',
            templateUrl: '/assets/templates/fileInput.html',
            scope: {
                content : '&',
                encoding : '@'
            },
            transclude : true,
            link: function(scope,element,attr){
                element.find("button").on("click", function(){
                    element.find("input").click();
                });

                element.find("input").on("change",function(){
                    if ($(this).val() != "") {
                        var files = $(this).get(0).files;
                        for (var i = 0; i < files.length; i++) {
                            var file = files[i];
                            var reader = new FileReader();
                            reader.onload = (function (fileName) {
                                return function (f) {
                                    var content = f.target.result;
                                    scope.content({data :
                                        {
                                        fileName : fileName,
                                        fileContent : content,
                                        encoding : (attr.encoding == undefined) ? 'UTF-8' : attr.encoding
                                        }
                                    });
                                }
                            })(file.name);
                            reader.readAsText(files[i], (attr.encoding == undefined) ? 'UTF-8' : attr.encoding);
                        }
                    }
                });
            }
        };
    });
})();