const { Core } = require('@adobe/aio-sdk');

function json(statusCode, body) {
  return { statusCode, headers: { 'Content-Type': 'application/json' }, body };
}

async function main(params) {
  const logger = Core.Logger('on-content-modified', { level: params.LOG_LEVEL || 'info' });

  const event = params.event || {};
  const path = event.data && event.data.payload && event.data.payload.path;

  if (!path) {
    logger.warn('Ignoring event without data.payload.path');
    return json(400, { error: 'Invalid event payload: data.payload.path is required' });
  }

  const host = (params.aem_host || '').replace(/\/$/, '');
  if (!host) {
    logger.error('aem_host is not configured');
    return json(500, { error: 'aem_host is not configured' });
  }

  const auth = params.aem_token
    ? `Bearer ${params.aem_token}`
    : (params.__ow_headers && params.__ow_headers.authorization) || '';
  if (!auth) {
    logger.error('No AEM token available');
    return json(401, { error: 'No authorization token available' });
  }

  const types = (params.analysis_types || 'sentiment,compliance')
    .split(',')
    .map((t) => t.trim())
    .filter(Boolean);

  try {
    const response = await fetch(`${host}/bin/translategemma/analyze`, {
      method: 'POST',
      headers: { Authorization: auth, 'Content-Type': 'application/json' },
      body: JSON.stringify({ path, types })
    });

    if (!response.ok) {
      const text = await response.text();
      logger.error(`AEM analyze failed (${response.status}): ${text}`);
      return json(502, { error: 'AEM analyze request failed', status: response.status });
    }

    const result = await response.json();
    logger.info(`Analyzed ${path}`);
    return json(200, { message: 'Analysis complete', path, result });
  } catch (error) {
    logger.error(`Request to AEM failed: ${error.message}`);
    return json(502, { error: 'Request to AEM failed' });
  }
}

exports.main = main;
