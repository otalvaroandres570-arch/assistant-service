-- KEYS[1] = hash with the number of active patients per level
-- KEYS[2] = key with the last known "LEVEL|STATUS" of the patient
-- ARGV[1] = level of the event, ARGV[2] = status of the event
-- ARGV[3] = seconds to remember a discharged patient
local ranks = { REGISTRADO = 1, EN_CONSULTA = 2, ALTA = 3 }

local newLevel = ARGV[1]
local newStatus = ARGV[2]
local previous = redis.call('GET', KEYS[2])

if previous then
  local previousLevel, previousStatus = string.match(previous, '^(.-)|(.+)$')

  -- older than what is already known: ignore (out-of-order delivery)
  if ranks[previousStatus] > ranks[newStatus] then
    return 0
  end
  -- same state again: ignore (duplicate, or an escalation, which does not change the state)
  if previousLevel == newLevel and previousStatus == newStatus then
    return 0
  end
  if previousStatus ~= 'ALTA' then
    redis.call('HINCRBY', KEYS[1], previousLevel, -1)
  end
end

if newStatus == 'ALTA' then
  redis.call('SET', KEYS[2], newLevel .. '|' .. newStatus, 'EX', tonumber(ARGV[3]))
else
  redis.call('HINCRBY', KEYS[1], newLevel, 1)
  redis.call('SET', KEYS[2], newLevel .. '|' .. newStatus)
end
return 1