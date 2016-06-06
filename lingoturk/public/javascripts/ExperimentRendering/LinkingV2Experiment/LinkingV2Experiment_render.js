(function () {
    var app = angular.module('LinkingV2ExperimentApp', []);

    app.controller('RenderController', ['$http', '$timeout', function ($http, $timeout) {
        var self = this;
        this.workerId = -1;
        this.assignmentId = -1;
        this.isDone = false;

        this.script1 = {};
        this.lhs_slot = -1;
        this.script2 = {};
        this.rhs_slot = -1;
        this.currentActiveItem = null;

        this.startingElement = null;
        this.connectedElementsMap = {};

        this.itemToDomElement = {};

        this.canvas = null;
        this.ctx = null;

        this.results = [];
        this.dragging = false;

        this.submitted = false;

        this.submit = function () {
            if(self.submitted){
                return;
            }

            self.submitted = true;

            var assignmentId = $("#assignmentId").val();
            var hitId = $("#hitId").val();
            var workerId = $("#workerId").val();
            var script_lhsId = self.script1.id;
            var script_rhsId = self.script2.id;

            var result = $(self.currentActiveItem.connectedElements[0]).attr("id");
            $("#result").val(result);

            var result = {
                experimentType : "LinkingV2Experiment",
                assignmentId: assignmentId,
                hitId: hitId,
                result : result,
                workerId: workerId,
                script_lhsId: script_lhsId,
                lhs_slot : self.lhs_slot,
                rhs_slot : self.rhs_slot,
                script_rhsId: script_rhsId
            };

            $http.post("/submitResults", result)
                .success(function () {
                    $("#form").submit();
                })
                .error(function () {
                    $("#form").submit();
                });
        };

        this.mousedown = function (event, item) {
            if(self.currentActiveItem.connectedElements.length == 0){
                self.dragging = true;
                $("#leftBox").parent().addClass("transparent-scrollbar");
            }
        };

        this.mouseup = function (event, item) {

            if (self.startingElement == null || !self.dragging) {
                return;
            }

            var connectedElements = self.currentActiveItem.connectedElements;

            if (connectedElements.indexOf(event.target) == -1) {
                if (item != undefined) {
                    self.connectedElementsMap[event.target] = item;
                } else {
                    self.connectedElementsMap[event.target] = {slot: $(event.target).attr("id")};
                }

                if(connectedElements.length == 0){
                    connectedElements.push(event.target);
                    $(event.target).addClass("force-visibility");
                }
            }

            self.renderLines();
            $("#leftBox").parent().removeClass("transparent-scrollbar");
            self.dragging = false;
        };

        this.isDone = function () {
            if (self.script1.items != undefined) {
                    if (self.currentActiveItem.connectedElements.length == 0) {
                        return false;
                    }else{
                        return true;
                    }
            }
            return false;
        };

        this.lastTime = Date.now();
        this.renderLines = function (event) {
            if(self.currentActiveItem != null && (Date.now() > (self.lastTime + 20))){
                var connectedElements = self.currentActiveItem.connectedElements;
                self.ctx.clearRect(0, 0, self.canvas.width, self.canvas.height);
                var context = self.ctx;
                var startingE = $(self.startingElement);

                for (var i = 0; i < connectedElements.length; i++) {
                    var element = $(connectedElements[i]);

                    context.beginPath();
                    context.moveTo(startingE.offset().left + startingE.innerWidth() - 10, startingE.offset().top + (startingE.innerHeight() / 2));
                    context.lineTo(element.offset().left, element.offset().top + (element.innerHeight() / 2));
                    context.stroke();
                }

                if (self.dragging && event != undefined) {
                    context.beginPath();
                    context.moveTo(startingE.offset().left + startingE.innerWidth() - 10, startingE.offset().top + (startingE.innerHeight() / 2));
                    context.lineTo(event.pageX, event.pageY);
                    context.stroke();
                }

                self.lastTime = Date.now();
            }
        };


        this.mousemove = function (event) {
            if (self.ctx != null && self.currentActiveItem != null) {
                self.renderLines(event);
            }
        };

        this.isNear = function (elementArray, distance) {
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

        this.checkItemClass = function (side,item) {
            var domE = $("#" + item.id)[0];
            if(side == "lhs"){
                if(item.slot == self.lhs_slot){
                    self.itemToDomElement[item.id] = domE;
                    self.startingElement = domE;
                    self.currentActiveItem = item;

                    self.connectedElementsMap[domE] = item;


                    return "item-current";
                }else{
                    return "item-active";
                }
            }else if(side == "rhs"){
                if(item.slot == self.rhs_slot){
                    self.itemToDomElement[item.id] = domE;
                    self.renderLines();

                    return "item-current";
                }else{
                    return "item-active";
                }
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
            if (!Date.now) {
                Date.now = function() { return new Date().getTime(); }
            }

            var story1 = $("#questionId").val();
            var story2 = $("#questionId2").val();
            self.lhs_slot = parseInt($("#lhs_slot").val());
            self.rhs_slot = parseInt($("#rhs_slot").val());

            if (story1 != "" && story2 != "") {
                $http.get("/getQuestion/" + story1).success(function (story1) {
                    $http.get("/getQuestion/" + story2).success(function (story2) {
                        self.script1 = story1;
                        self.script2 = story2;

                        for (var i = 0; i < self.script1.items.length; i++) {
                            self.script1.items[i].connectedElements = [];

                            if(self.script1.items[i].slot == self.lhs_slot){
                                self.currentActiveItem = self.script1.items[i];
                            }
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
                            self.canvas.height = document.body.clientHeight * 0.90;
                            self.renderLines();
                        });

                        self.canvas = document.getElementById("canvas");
                        self.canvas.width = document.body.clientWidth;
                        self.canvas.height = document.body.clientHeight * 0.90;
                        self.ctx = self.canvas.getContext("2d");
                    });
                });
            }


        })


    }]);
})();
