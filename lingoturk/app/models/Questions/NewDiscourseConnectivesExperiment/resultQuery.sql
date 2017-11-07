SELECT fileName, listNumber, NewDiscourseConnectives_sentence1, NewDiscourseConnectives_sentence2, NewDiscourseConnectives_context1, NewDiscourseConnectives_context2, NewDiscourseConnectives_condition, NewDiscourseConnectives_WSJID, NewDiscourseConnectives_relation, NewDiscourseConnectives_pdtbConn, NewDiscourseConnectives_pdtbSense, NewDiscourseConnectives_rstSense, NewDiscourseConnectives_nr, assignmentId, hitId, workerId, origin, timestamp, partId, questionId, connective1, connective2, manualAnswer1, manualAnswer2 FROM (
	SELECT * FROM NewDiscourseConnectivesResults
	LEFT OUTER JOIN Questions USING (QuestionId)
	LEFT OUTER JOIN Groups USING (PartId)
) as tmp
WHERE LingoExpModelId = 681
ORDER BY partId, questionId, workerId