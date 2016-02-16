(function () {
    var app = angular.module('guessing', []);

    app.controller('RenderController', ['$http', '$timeout', function ($http, $timeout) {
        var self = this;
        this.workerId = -1;
        this.assignmentId = -1;
        this.isDone = false;

        this.script1 = {};
        this.script2 = {};
        this.currentActiveItem = null;

        this.startingElement = null;
        this.connectedElementsMap = {};

        this.itemToDomElement = {};

        this.canvas = null;
        this.ctx = null;

        this.results = [];
        this.dragging = false;

        this.submitted = false;

        this.beginning = 0;

        this.submit = function () {
            if (self.submitted) {
                return;
            }

            self.submitted = true;

            var wTime = (performance.now() - self.beginning) / 1000;
            var assignmentId = $("#assignmentId").val();
            var hitId = $("#hitId").val();
            var workerId = $("#workerId").val();
            var script_lhsId = self.script1.id;
            var script_rhsId = self.script2.id;

            var results = [];

            for (var i = 0; i < self.script1.items.length; i++) {
                var item = self.script1.items[i];
                for (var j = 0; j < item.connectedElements.length; j++) {
                    var rhs = $(item.connectedElements[j]).attr("id");
                    results.push({lhs: item.id, rhs: rhs});
                    var newInput = $("<input type='hidden' name='" + item.id + "' value='" + rhs + "'>");
                    $("form").append(newInput);
                }
            }

            var result = {
                experimentType : "LinkingExperimentV1",
                workingTimes: wTime,
                assignmentId: assignmentId,
                hitId: hitId,
                workerId: workerId,
                script_lhsId: script_lhsId,
                script_rhsId: script_rhsId,
                results: results
            };

            $http.post("/submitResults", result)
                .success(function () {
                    $("#form").submit();
                    //parent.postMessage("FINISHED",document.location.origin);
                })
                .error(function () {
                    setTimeout(function () {
                        self.submit()
                    }, 2000);
                });
        };

        this.mousedown = function (event, item) {
            self.startingElement = event.target;
            self.connectedElementsMap[event.target] = item;
            var connectedElements = self.currentActiveItem.connectedElements;

            if ((connectedElements.length == 1 && isNaN($(connectedElements[0]).attr("id")))) {
                return;
            }

            self.dragging = true;
            $("#leftBox").parent().addClass("transparent-scrollbar");
        };

        this.mouseup = function (event, item) {
            var connectedElements = self.currentActiveItem.connectedElements;

            if (self.startingElement == null || !self.dragging ||
                (connectedElements.length != 0 && item === undefined)) {
                return;
            }

            if (connectedElements.indexOf(event.target) == -1) {
                if (item != undefined) {
                    self.connectedElementsMap[event.target] = item;
                } else {
                    self.connectedElementsMap[event.target] = {slot: $(event.target).attr("id")};
                }
                connectedElements.push(event.target);
                $(event.target).addClass("force-visibility");
            }

            self.renderLines();
            $("#leftBox").parent().removeClass("transparent-scrollbar");
            self.dragging = false;
        };

        this.checkItem = function (item) {
            if (item.isActive && item.connectedElements.length > 0 && self.currentActiveItem != item) {
                return true;
            } else {
                return false;
            }
        };

        this.checkItemProgress = function (item) {
            if (item.isActive && item.connectedElements.length > 0 && self.currentActiveItem == item) {
                return true;
            } else {
                return false;
            }
        };

        this.isDone = function () {
            if (self.script1.items != undefined) {
                for (var i = 0; i < self.script1.items.length; i++) {
                    var tmpItem = self.script1.items[i];
                    if (tmpItem.isActive && tmpItem.connectedElements.length == 0) {
                        return false;
                    }
                }
                return true;
            }
            return false;
        };

        this.renderLines = function () {
            if (self.currentActiveItem != null) {
                var connectedElements = self.currentActiveItem.connectedElements;
                self.ctx.clearRect(0, 0, self.canvas.width, self.canvas.height);
                var context = self.ctx;
                var startingE = $(self.startingElement);

                for (var i = 0; i < connectedElements.length; i++) {
                    var element = $(connectedElements[i]);

                    context.beginPath();
                    context.moveTo(startingE.offset().left + startingE.innerWidth() - 10, startingE.offset().top + (startingE.innerHeight() / 2));
                    context.lineTo(element.offset().left, element.offset().top + (element.innerHeight() / 2));
                    context.strokeStyle = '#000000';
                    context.stroke();
                }

                for (var i = 0; i < self.script1.items.length; i++) {
                    var item = self.script1.items[i];
                    if (item != self.currentActiveItem) {
                        startingE = $(self.itemToDomElement[item.slot]);

                        for (var j = 0; j < item.connectedElements.length; j++) {
                            var element = $(item.connectedElements[j]);

                            context.beginPath();
                            context.moveTo(startingE.offset().left + startingE.innerWidth() - 10, startingE.offset().top + (startingE.innerHeight() / 2));
                            context.lineTo(element.offset().left, element.offset().top + (element.innerHeight() / 2));
                            context.strokeStyle = '#B0B0B0';
                            context.stroke();
                        }
                    }
                }
            }
        };

        this.mousemove = function (event) {
            if (self.ctx != null && self.currentActiveItem != null) {
                self.renderLines();
                if (self.dragging) {
                    var context = self.ctx;
                    var startingE = $(self.startingElement);
                    context.beginPath();
                    context.moveTo(startingE.offset().left + startingE.innerWidth() - 10, startingE.offset().top + (startingE.innerHeight() / 2));
                    context.lineTo(event.pageX, event.pageY);
                    context.strokeStyle = '#000000';
                    context.stroke();
                }
            }
        };

        this.setCurrentActiveItem = function (item, event) {
            if (!item.isActive) {
                return;
            }

            var domElement = event.target;
            if (item != undefined && event != undefined && self.itemToDomElement[item.slot] == undefined) {
                self.itemToDomElement[item.slot] = domElement;
            }

            var connectedElements = null;
            if (self.currentActiveItem != null) {
                connectedElements = self.currentActiveItem.connectedElements;
                for (var i = 0; i < connectedElements.length; i++) {
                    $(connectedElements[i]).removeClass("force-visibility");
                }
            }
            self.startingElement = event.target;
            self.currentActiveItem = item;

            connectedElements = self.currentActiveItem.connectedElements;
            for (var i = 0; i < connectedElements.length; i++) {
                $(connectedElements[i]).addClass("force-visibility");
            }

            self.renderLines();
        };

        this.isNear = function (elementArray, distance, event) {
            for (var i = 0; i < elementArray.length; i++) {
                var element = $(elementArray[i]);

                var left = element.offset().left - distance,
                    top = element.offset().top - distance,
                    right = left + element.width() + 2 * distance,
                    bottom = top + element.height() + 2 * distance,
                    x = event.pageX,
                    y = event.pageY;

                if (element.hasClass("force-visibility") || (x > left && x < right && y > top && y < bottom)) {
                    element.css("opacity", "1");
                    element.css("width", "20%");
                    element.css("height", "26px");
                    element.css("margin-bottom", "10px");
                } else {
                    element.css("opacity", "0.8");
                    element.css("width", "20%");
                    element.css("height", "initial");
                    element.css("margin-bottom", "10px");
                }
            }
            return false;
        };

        this.update = function () {
            var scope = angular.element($("#angularApp")).scope();
            $timeout(function () {
                scope.$apply();
            });
        };

        this.checkItemClass = function (item) {
            if (item == self.currentActiveItem) {
                return "item-current";
            } else if (item.isActive) {
                return "item-active";
            } else {
                return "item-inactive";
            }
        };

        this.removeConenction = function (event) {
            var connectedElements = self.currentActiveItem.connectedElements;
            var element = $(event.target).parent()[0];
            var index = connectedElements.indexOf(element);
            if (index > -1) {
                $(element).removeClass("force-visibility");
                connectedElements.splice(index, 1);
            }
        };

        $(document).ready(function () {
            var story1 = $("#story1").val();
            var story2 = $("#story2").val();

            if (story1 != "" && story2 != "") {
                $http.get("/getQuestion/" + story1).success(function (story1) {
                    $http.get("/getQuestion/" + story2).success(function (story2) {
                        self.script1 = story1;
                        self.script2 = story2;

                        for (var i = 0; i < self.script1.items.length; i++) {
                            var tmpItem = self.script1.items[i];
                            tmpItem.connectedElements = [];
                        }

                        self.update();

                        //http://stackoverflow.com/questions/7911604/function-for-mouse-near-an-element-in-jquery
                        $(document).mousemove(function (event) {
                            self.isNear($(".blank"), 10, event);
                        });

                        $(document).mouseup(function () {
                            $("#leftBox").parent().removeClass("transparent-scrollbar");
                            self.dragging = false;
                        });

                        $(".panel").scroll(function () {
                            self.renderLines();
                        });

                        $(window).resize(function () {
                            self.canvas = document.getElementById("canvas");
                            self.canvas.width = document.body.clientWidth;
                            self.canvas.height = document.body.clientHeight;
                            self.renderLines();
                        });

                        self.canvas = document.getElementById("canvas");
                        self.canvas.width = document.body.clientWidth;
                        self.canvas.height = document.body.clientHeight;
                        self.ctx = self.canvas.getContext("2d");

                        self.beginning = performance.now();
                    });
                });
            }


        })


    }]);
})();
