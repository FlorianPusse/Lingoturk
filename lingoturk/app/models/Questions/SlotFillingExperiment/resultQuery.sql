SELECT fileName, listNumber, SlotFilling_itemID, SlotFilling_listID, SlotFilling_condition, SlotFilling_story, SlotFilling_rightAnswer, assignmentId, hitId, workerId, origin, timestamp, partId, questionId, statistics, answer FROM (
	SELECT * FROM SlotFillingResults
	LEFT OUTER JOIN Questions USING (QuestionId)
	LEFT OUTER JOIN Groups USING (PartId)
) as tmp
WHERE LingoExpModelId = 161
ORDER BY partId, questionId, workerId