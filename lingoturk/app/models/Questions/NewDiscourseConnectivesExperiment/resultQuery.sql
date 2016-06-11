SELECT NewDiscourseConnectivesResults.*, NewDiscourseConnectives_relation, NewDiscourseConnectives_nr, NewDiscourseConnectives_WSJID
FROM
	NewDiscourseConnectivesResults
JOIN
	Questions
USING (questionId)
ORDER BY id;