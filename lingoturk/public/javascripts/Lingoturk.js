(function () {
    var app = angular.module('Lingoturk', []);

    /* http://stackoverflow.com/questions/9381926/angularjs-insert-html-into-view */
    app.filter('unsafe', function ($sce) {
        return $sce.trustAsHtml;
    });

    app.directive("statisticsSlide", function ($compile) {
        return {
            restrict: 'E',
            templateUrl: '/assets/templates/statisticsDirective.html',
            scope: {
                statistics: '=',
                click: '=',
                content: '='
            },
            link: function (scope, element, attrs) {
                scope.statisticsValid = function () {
                    for (var i = 0; i < scope.statistics.length; ++i) {
                        var statistic = scope.statistics[i];
                        if ((statistic.answer === undefined || (statistic.answer == "" && statistic.type == "text")) && (statistic.optional === undefined || statistic.optional == false)) {
                            return false;
                        }
                    }
                    return true;
                };
            }
        };
    });

    app.directive("workerSlide", function ($compile) {
        return {
            restrict: 'E',
            templateUrl: '/assets/templates/workeridDirective.html',
            scope: {
                content: '='
            },
            compile: function (tElement, tAttr) {
                var contents = tElement.contents().remove();
                var compiledContents;
                return function (scope, iElement, iAttr) {
                    if (!compiledContents) {
                        compiledContents = $compile(contents);
                    }
                    compiledContents(scope, function (clone, scope) {
                        iElement.append(clone);
                    });
                };
            }
        };
    });

    app.directive("slide", function ($compile) {
        return {
            restrict: 'E',
            templateUrl: '/assets/templates/slide.html',
            scope: {
                content: '='
            },
            compile: function (tElement, tAttr) {
                var contents = tElement.contents().remove();
                var compiledContents;
                return function (scope, iElement, iAttr) {
                    if (!compiledContents) {
                        compiledContents = $compile(contents);
                    }
                    compiledContents(scope, function (clone, scope) {
                        iElement.append(clone);
                    });
                };
            }
        };
    });

    app.directive("instructions", function ($compile) {
        return {
            restrict: 'E',
            templateUrl: '/assets/templates/slide.html',
            scope: {
                content: '='
            },
            compile: function (tElement, tAttr) {
                var contents = tElement.contents().remove();
                var compiledContents;
                return function (scope, iElement, iAttr) {
                    if (!compiledContents) {
                        compiledContents = $compile(contents);
                    }
                    compiledContents(scope, function (clone, scope) {
                        iElement.append(clone);
                    });
                };
            }
        };
    });

    app.directive("textAnswer", function () {
        return {
            restrict: 'E',
            templateUrl: '/assets/templates/textAnswerPanel.html',
            scope: {
                answer: '=',
                restrictAnswer: '@?'
            },
            link: function (scope, element, attrs) {
                scope.answer_tmp = "";

                $(element).find("input").on("input", function () {
                    if (scope.hasOwnProperty('restrictAnswer')) {
                        scope.matches = new RegExp(attrs.restrictAnswer).test(scope.answer_tmp);
                        if (scope.matches) {
                            scope.$apply(scope.answer = scope.answer_tmp);
                        } else {
                            scope.$apply(scope.answer = '');
                        }
                    } else {
                        scope.answer = scope.answer_tmp;
                    }
                })
            }
        };
    });

    app.directive("radioAnswer", function () {
        return {
            restrict: 'E',
            templateUrl: '/assets/templates/radioAnswerPanel.html',
            scope: {
                answer: '=',
                options: '=',
                inline: '=?'
            },
            link: function (scope, element, attrs) {
                scope.setAnswer = function (a) {
                    scope.answer = a;
                }
            }
        };
    });

    app.directive("checkboxAnswer", function () {
        return {
            restrict: 'E',
            templateUrl: '/assets/templates/checkboxAnswerPanel.html',
            scope: {
                answer: '=',
                options: '=',
                descriptions: '=?',
                addNone: '@?'
            },
            link: function (scope, element, attrs) {
                if (!scope.hasOwnProperty("answer") || typeof(scope.answer) === "undefined") {
                    scope.answer = {};
                }
                scope.addNone = scope.hasOwnProperty("addNone") && scope.addNone == "true";
                for (var i = 0; i < scope.options.length; ++i) {
                    scope.answer[scope.options[i]] = false;
                }
                if (scope.addNone) {
                    scope.answer['none'] = false;
                }
                scope.descriptionsAvailable = scope.hasOwnProperty("descriptions");
                scope.setAnswer = function (a) {
                    scope.answer = a;
                };
                scope.loaded = function () {
                    $('[data-toggle="tooltip"]').tooltip({delay: 0});
                }
            }
        };
    });

    app.directive("starAnswer", function () {
        return {
            restrict: 'E',
            templateUrl: '/assets/templates/starAnswerPanel.html',
            scope: {
                answer: '=',
                maxStars: '=?'
            },
            link: function (scope, element, attrs) {
                if (scope.maxStars === undefined) {
                    scope.maxStars = 5;
                }

                scope.range = function (max) {
                    var result = [];
                    for (var i = 0; i < max; ++i) {
                        result.push(i);
                    }
                    return result;
                };

                scope.setAnswer = function (i) {
                    scope.answer = i;
                };
            }
        };
    });

    app.directive("sliderAnswer", function () {
        return {
            restrict: 'E',
            templateUrl: '/assets/templates/sliderAnswerPanel.html',
            scope: {
                answer: '=',
                useScale: '@?'
            },
            link: function (scope, element, attrs) {
                scope.activated = false;
                if (!scope.hasOwnProperty("useScale")) {
                    scope.useScale = "scale";
                }

                var elem = $(element[0]).find(".slider");
                elem.slider({
                    min: 0,
                    max: 100,
                    change: function (event, ui) {
                        scope.$apply(scope.answer = ui.value);
                        scope.$apply(scope.activated = true);
                        elem.find(".ui-slider-handle").show();
                    }
                })
                    .each(function () {
                        if (scope.useScale === "scale") {
                            /* http://stackoverflow.com/questions/10224856/jquery-ui-slider-labels-under-slider */
                            var opt = elem.slider("option");
                            var vals = opt.max - opt.min;

                            for (var i = 0; i <= vals; i += 10) {
                                var el = $('<label style="position: absolute; width: 20px; margin-top: 20px; margin-left: -10px;text-align: center;">' + (i + opt.min) + '%</label>').css('left', (i / vals * 100) + '%');
                                elem.append(el);
                            }
                        } else {
                            var leftLabel = $("<label style=\"position: absolute;width: 20px;margin-top: 20px;margin-left: -10px;text-align: center;\">extremely unlikely</label>");
                            var middleLabel = $("<label style=\"width: 100%;margin-top: 20px;text-align: center;\">moderately likely</label>");
                            var rightLabel = $("<label style=\"position: absolute; width: 20px; margin-top: 20px; margin-left: -60px; text-align: center; left: 100%;\">extremely likely</label>");

                            elem.append(leftLabel);
                            elem.append(middleLabel);
                            elem.append(rightLabel);
                        }
                    });
                elem.find(".ui-slider-handle").hide();
            }
        };
    });


    app.directive("dragAndDropMultipleDecisions", ['$timeout', function ($timeout) {
        return {
            restrict: 'E',
            templateUrl: '/assets/templates/dragAndDropV1.html',
            scope: {
                content: '=',
                answer: '=',
                click: '='
            },
            link: function (scope, element, attrs) {
                scope.chosenCategories = [];
                scope.categorySet = false;
                scope.connectives = [];
                scope.categories = [];
                scope.manualAnswer = "";
                scope.noneOfThese = false;
                scope.suitableCategory = "";

                scope.answer = {};

                for (var attribute in attrs) {
                    if (attrs.hasOwnProperty(attribute) && attribute.startsWith('category')) {
                        var attributeName = attribute.replace('category', '');
                        var cat = {category: attributeName};
                        var connectives = attrs[attribute].split(",");
                        $.map(connectives, $.trim);
                        cat['connectives'] = connectives;
                        scope.categories.push(cat);
                    }
                }

                scope.getAnswers = function (name) {
                    var answer = [];
                    $(name).find("li").each(function (index, element) {
                        answer.push($(this).text().trim());
                    });
                    return answer;
                };

                scope.finished = function () {
                    scope.answer.chosenCategories = scope.chosenCategories;
                    scope.answer.notRelevant = scope.getAnswers("#sortableNotRelevant");
                    scope.answer.cantDecide = scope.getAnswers("#sortableCantDecide");

                    for (var i = 1; ; ++i) {
                        var name = "#sortableValidConnectives_" + i;
                        if ($(name).length > 0) {
                            scope.answer["validConnectives_" + i] = scope.getAnswers(name);
                        } else {
                            break;
                        }
                    }

                    scope.click();
                };

                /*
                 *	Taken from: http://stackoverflow.com/questions/20789373/shuffle-array-in-ng-repeat-angular
                 *	-> Fisher–Yates shuffle algorithm
                 */
                scope.shuffleArray = function (array) {
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

                scope.setNoneOfThese = function (value) {
                    scope.noneOfThese = value;
                    scope.update();

                    if (value == false) {
                        scope.manualAnswer = "";
                        $("#submitButton").attr("disabled", "disabled");
                        scope.update();
                    } else {
                        $("#submitButton").removeAttr("disabled");
                    }

                };

                scope.update = function () {
                    var scope = angular.element($("#angularApp")).scope();
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

                scope.testCollision = function (item, list) {
                    var found = false;
                    list.each(function () {
                        if (scope.collision(item, $(this))) {
                            found = true;
                            return true;
                        }
                    });
                    if (found) {
                        return true;
                    }
                    return false;
                };

                scope.removeSuitableCategory = function (suitableCategory) {
                    var index = scope.chosenCategories.indexOf(suitableCategory);
                    if (index > -1) {
                        scope.chosenCategories.splice(index, 1);
                        scope.updateConnectives();
                    }
                    if (scope.chosenCategories.length == 0) {
                        scope.categoryIsSet(false);
                    }
                };

                scope.updateConnectives = function () {
                    var tmp = [];
                    for (var i = 0; i < scope.categories.length; i++) {
                        if ($.inArray(scope.categories[i].category, scope.suitableCategories) != -1) {
                            tmp = tmp.concat(scope.categories[i].connectives);
                        }
                    }

                    scope.connectives = tmp;
                    $("#sortableBody").css('background-color', '#ffe7c7');
                    scope.update();
                };

                scope.categoriesChosen = function () {
                    scope.suitableCategories = scope.chosenCategories;
                    scope.updateConnectives();

                    $("#sortableBody").css('background-color', '#ffe7c7');
                    scope.categoryIsSet(true);
                };

                scope.addValidConnectivesCategory = function () {
                    var index = $("#validConnectivesContainer > div").length + 1;
                    var code = $("<div> <div class='panel panel-warning' > <div class='panel-heading' style='color:#000000'>valid connective</div> <div class='panel-body' style='background-color: #DFFFBF;'> <ul style='margin-bottom:0px' class='list-inline sortable sortableValidConnectives' id='sortableValidConnectives_" + index + "' > </ul> </div> </div> </div>");
                    $("#validConnectivesContainer > div").last().after(code);
                    scope.update();
                    scope.initConnectivesSortable();
                };

                scope.initConnectivesSortable = function () {
                    $(".sortable").sortable({
                        connectWith: ".sortable",
                        items: ".draggable",
                        revert: true,
                        sort: function (event, ui) {
                            if (scope.collision(ui.item, $("#sortableConnectives")) || scope.collision(ui.item, $("#sortableNotRelevant")) || scope.collision(ui.item, $("#sortableCantDecide")) || scope.testCollision(ui.item, $(".sortableValidConnectives"))) {
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
                            if ($(event.target).attr("id") == "suitableCategory") {
                                scope.chosenCategories.push(connective);
                                $("#categoriesChosenButton").removeAttr("disabled");
                            } else {
                                scope.chosenCategories.splice(scope.chosenCategories.indexOf(connective), 1);
                                $("#categoriesChosenButton").attr("disabled", "disabled");
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

                scope.categoryIsSet = function (value) {
                    scope.categorySet = value;
                    if (value == true) {
                        scope.update();
                        $timeout(scope.initConnectivesSortable, 1000);
                    } else {
                        $("#sortableBody").css('background-color', '#FFFFFF');
                        $("#submitButton").attr("disabled", "disabled");
                        scope.suitableCategory = "";
                        scope.manualAnswer = "";
                        scope.chosenCategories = [];
                        scope.suitableCategories = [];
                        scope.update();
                        $timeout(scope.initCategorySortable, 1000);
                    }
                    scope.update();
                };

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

                $(element[0]).on("keypress", ":input:not(textarea)", function (event) {
                    if (event.keyCode == 13) {
                        event.preventDefault();
                    }
                });

                $timeout(scope.initCategorySortable, 1000);
            }
        };
    }]);

    app.directive("dragAndDrop", ['$timeout', function ($timeout) {
        return {
            restrict: 'E',
            templateUrl: '/assets/templates/dragAndDrop.html',
            scope: {
                content: '=',
                answer: '=',
                click: '=',
                connectives: '@',
                context1: '=?',
                context2: '=?',
                sentence1: '=',
                sentence2: '=',
                randomizeConnectives: '=?',
                instructions1: '@?',
                instructions2: '@?',
                boxTitle: '@?',
                allowMultiple: '@?',
                goBackText: '@?',
                noneStyle: '@?'
            },
            link: function (scope, element, attrs) {
                var tmp = scope.connectives.split(",");
                if (scope.hasOwnProperty('randomizeConnectives') && scope.randomizeConnectives == true) {
                    shuffleArray(tmp);
                }
                if (!scope.hasOwnProperty('instructions1')) {
                    scope.instructions1 = "Please drag the best-suited connective into the green target box below.";
                }
                if (!scope.hasOwnProperty('instructions2')) {
                    scope.instructions2 = "You can now drag one more connective into the box below.";
                }
                if (!scope.hasOwnProperty('boxTitle')) {
                    scope.boxTitle = "Candidate connectives";
                }
                if (!scope.hasOwnProperty("allowMultiple")) {
                    scope.allowMultiple = "true";
                }
                if (!scope.hasOwnProperty("goBackText")) {
                    scope.goBackText = "I changed my mind";
                }
                if (!scope.hasOwnProperty("noneStyle")) {
                    scope.goBackText = "block";
                }
                if (!scope.hasOwnProperty("context1")) {
                    scope.context1 = "";
                }
                if (!scope.hasOwnProperty("context2")) {
                    scope.context2 = "";
                }

                scope.answer = {};

                scope.processedConnectives = $.map(tmp, $.trim);
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

                scope.testCollision = function (item, list) {
                    var found = false;
                    list.each(function () {
                        if (scope.collision(item, $(this))) {
                            found = true;
                            return true;
                        }
                    });
                    if (found) {
                        return true;
                    }
                    return false;
                };

                scope.setAnswers = function () {
                    if (scope.suitableCategories.length > 0) {
                        scope.answer.connective1 = scope.suitableCategories[0];
                        if (scope.chosenConnectives.length > 0) {
                            scope.answer.connective2 = scope.chosenConnectives[0];
                        }
                    } else {
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
                            if ($(event.target).attr("id") == "secondPhase") {
                                scope.chosenConnectives.push(connective);
                            } else {
                                scope.chosenConnectives.splice(scope.chosenConnectives.indexOf(connective), 1);
                            }
                            if (scope.chosenConnectives.length == 1) {
                                $("#sortableConnectives").css("pointer-events", "none");
                                $("#sortableConnectives").parent().addClass("disabledSortable");
                            } else {
                                $("#sortableConnectives").css("pointer-events", "all");
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
                            if ($(event.target).attr("id") == "suitableCategory") {
                                scope.chosenConnectives.push(connective);
                                $(".continue-button").removeAttr("disabled");
                            } else {
                                scope.chosenConnectives.splice(scope.chosenConnectives.indexOf(connective), 1);
                                $(".continue-button").attr("disabled", "disabled");
                            }
                            if (scope.chosenConnectives.length == 1) {
                                $("#category").css("pointer-events", "none");
                                $("#category").parent().addClass("disabledSortable");
                            } else {
                                $("#category").css("pointer-events", "all");
                                $("#category").parent().removeClass("disabledSortable");
                            }
                        }
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

                $timeout(scope.initCategorySortable, 1000);
            }
        };
    }]);

    app.directive("fileInput", function () {
        return {
            restrict: 'E',
            templateUrl: '/assets/templates/fileInput.html',
            scope: {
                content: '&',
                encoding: '@'
            },
            transclude: true,
            link: function (scope, element, attr) {
                element.find("button").on("click", function () {
                    element.find("input").click();
                });

                element.find("input").on("change", function () {
                    if ($(this).val() != "") {
                        var files = $(this).get(0).files;
                        for (var i = 0; i < files.length; i++) {
                            var file = files[i];
                            var reader = new FileReader();
                            reader.onload = (function (fileName) {
                                return function (f) {
                                    var content = f.target.result;
                                    scope.content({
                                        data: {
                                            fileName: fileName,
                                            fileContent: content,
                                            encoding: (attr.encoding == undefined) ? 'UTF-8' : attr.encoding
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