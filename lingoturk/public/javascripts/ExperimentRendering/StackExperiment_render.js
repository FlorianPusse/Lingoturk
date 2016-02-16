(function () {
    var app = angular.module('StackExperiment', []);

    app.controller('RenderController', ['$http', '$timeout', function ($http, $timeout) {
        var self = this;
        this.workerId = -1;
        this.assignmentId = -1;
        this.words = [];
        this.stacks = [];

        this.currentActiveItem = null;
        this.startingElement = null;
        this.itemToDomElement = {};

        this.connectedElementsMap = [];
        this.dragging = false;
        this.canvas = null;
        this.ctx = null;

        this.mousedown = function (event, item) {
            this.itemToDomElement[item.id] = event.target;

            self.startingElements = $(".ui-selected").toArray();
            self.currentActiveItem = item;
            self.connectedElementsMap[event.target] = item;
            self.dragging = true;
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
                connectedElements.push(event.target);
                $(event.target).addClass("force-visibility");
            }

            self.renderLines();
            $("#leftBox").parent().removeClass("transparent-scrollbar");
            self.dragging = false;
        };

        this.renderLines = function () {
            if(self.currentActiveItem != null){
                var connectedElements = self.currentActiveItem.connectedElements;
                self.ctx.clearRect(0, 0, self.canvas.width, self.canvas.height);
                var context = self.ctx;

                for(var sCounter = 0; sCounter < self.startingElements.length; sCounter++){
                    var startingE = $(self.startingElements[sCounter]);

                    for (var i = 0; i < connectedElements.length; i++) {
                        var element = $(connectedElements[i]);

                        context.beginPath();
                        context.moveTo(startingE.offset().left + startingE.innerWidth() - 10, startingE.offset().top + (startingE.innerHeight() / 2));
                        context.lineTo(element.offset().left, element.offset().top + (element.innerHeight() / 2));
                        context.strokeStyle = '#000000';
                        context.stroke();
                    }

                }

                for (var i = 0; i < self.words.length; i++) {
                    var word = self.words[i];
                    if (word != self.currentActiveItem) {
                        startingE = $(self.itemToDomElement[word.id]);

                        for (var j = 0; j < word.connectedElements.length; j++) {
                            var element = $(word.connectedElements[j]);

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

                    for(var sCounter = 0; sCounter < self.startingElements.length; sCounter++) {
                        var startingE = $(self.startingElements[sCounter]);
                        context.beginPath();
                        context.moveTo(startingE.offset().left + startingE.innerWidth() - 10, startingE.offset().top + (startingE.innerHeight() / 2));
                        context.lineTo(event.pageX, event.pageY);
                        context.strokeStyle = '#000000';
                        context.stroke();
                    }
                }
            }
        };

        this.update = function () {
            var scope = angular.element($("#angularApp")).scope();
            $timeout(function () {
                scope.$apply();
            });
        };

        this.Word = function(id,word){
            var self = this;
            self.id = id;
            self.word = word;
            self.connectedElements = [];
        };

        $(document).ready(function () {
            var jsonString = '{"sentenceAnnotatorId": "c8b53414-af1e-46e5-9185-c9b9febd2d56", "sentence": "Paranormal Activity von James Cameron kommt nicht im Passage-Kino, aber Iron Man l\u00e4uft, Tickets kosten 8 Euro 50.", "mandatoryStackListId": "reject42.xml", "stacks": [{"attribute": "theatre", "alternative": false, "discourseAct": "reject", "value": "passage", "representation": false}, {"attribute": "director", "alternative": false, "discourseAct": "reject", "value": "james cameron", "representation": false}, {"attribute": "name", "alternative": false, "discourseAct": "reject", "value": "paranormal activity", "representation": false}, {"attribute": "name", "alternative": true, "discourseAct": "reject", "value": "iron man", "representation": false}, {"attribute": "price", "alternative": false, "discourseAct": "inform", "value": "8.50", "representation": false}], "optionalStackAnnotatorId": null}';
            var json = JSON.parse(jsonString);

            var words = json.sentence.split(" ");
            for(var i = 0; i < words.length; i++){
                self.words.push(new self.Word(i,words[i]));
            }

            self.stacks = json.stacks;
            self.update();
            $("#selectable").selectable({
                cancel : ".ui-selected"
            });

            $(document).mouseup(function () {
                self.dragging = false;
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
        })

    }]);
})();
