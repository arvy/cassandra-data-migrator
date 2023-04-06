package datastax.cdm.feature;

import datastax.cdm.job.MigrateDataType;
import datastax.cdm.properties.KnownProperties;
import datastax.cdm.properties.PropertyHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ExplodeMap extends AbstractFeature {
    public Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    public enum Property {
        MAP_COLUMN_NAME,
        MAP_COLUMN_INDEX,
        MAP_COLUMN_TYPE,
        KEY_COLUMN_NAME,
        KEY_COLUMN_TYPE,
        VALUE_COLUMN_NAME,
        VALUE_COLUMN_TYPE
    }

    private boolean valid = true;

    @Override
    public boolean initialize(PropertyHelper helper) {
        String mapColumnName = helper.getString(KnownProperties.EXPLODE_MAP_ORIGIN_COLUMN_NAME);
        putString(Property.MAP_COLUMN_NAME, mapColumnName);

        String keyColumnName = helper.getString(KnownProperties.EXPLODE_MAP_TARGET_KEY_COLUMN_NAME);
        putString(Property.KEY_COLUMN_NAME, keyColumnName);

        String valueColumnName = helper.getString(KnownProperties.EXPLODE_MAP_TARGET_VALUE_COLUMN_NAME);
        putString(Property.VALUE_COLUMN_NAME, valueColumnName);

        if (isValidColumn(helper) && isMapType(helper)) {
            putNumber(Property.MAP_COLUMN_INDEX, helper.getStringList(KnownProperties.ORIGIN_COLUMN_NAMES).indexOf(mapColumnName));

            MigrateDataType columnMapDataType = getColumnMapDataType(helper);
            putMigrateDataType(Property.MAP_COLUMN_TYPE, columnMapDataType);
            putMigrateDataType(Property.KEY_COLUMN_TYPE, columnMapDataType.getSubTypeTypes().get(0));
            putMigrateDataType(Property.VALUE_COLUMN_TYPE, columnMapDataType.getSubTypeTypes().get(1));
        }

        valid = isValid(helper);
        isInitialized = true;
        isEnabled = valid && null!=mapColumnName && !mapColumnName.isEmpty();
        return valid;
    }

    @Override
    public PropertyHelper alterProperties(PropertyHelper helper) {
            if (!valid) return null;
            if (!isEnabled) return helper;

            clean_targetPKTypes(helper);
            clean_targetColumnNamesAndTypes(helper);
            return helper;
    }

    // The target PK can reference KEY_COLUMN_NAME and/or VALUE_COLUMN_NAME, but these could be
    // unknown types.
    private void clean_targetPKTypes(PropertyHelper helper) {
        String keyColumnName = getRawString(Property.KEY_COLUMN_NAME);
        String valueColumnName = getRawString(Property.VALUE_COLUMN_NAME);

        List<String> currentPKNames = helper.getStringList(KnownProperties.TARGET_PRIMARY_KEY);
        List<MigrateDataType> currentPKTypes = helper.getMigrationTypeList(KnownProperties.TARGET_PRIMARY_KEY_TYPES);

        List<MigrateDataType> newPKTypes = new ArrayList<>();

        for (int i=0; i<currentPKNames.size(); i++) {
            String pkName = currentPKNames.get(i);
            MigrateDataType pkType = currentPKTypes.get(i);

            if (pkName.equals(keyColumnName)) {
                newPKTypes.add(getRawMigrateDataType(Property.KEY_COLUMN_TYPE));
            } else if (pkName.equals(valueColumnName)) {
                newPKTypes.add(getRawMigrateDataType(Property.VALUE_COLUMN_TYPE));
            } else {
                newPKTypes.add(pkType);
            }
        }

        helper.setProperty(KnownProperties.TARGET_PRIMARY_KEY_TYPES, newPKTypes);
    }

    private void clean_targetColumnNamesAndTypes(PropertyHelper helper) {
        String mapColumnName = getRawString(Property.MAP_COLUMN_NAME);
        String keyColumnName = getRawString(Property.KEY_COLUMN_NAME);
        String valueColumnName = getRawString(Property.VALUE_COLUMN_NAME);

        List<String> currentColumnNames = helper.getStringList(KnownProperties.TARGET_COLUMN_NAMES);
        List<MigrateDataType> currentColumnTypes = helper.getMigrationTypeList(KnownProperties.TARGET_COLUMN_TYPES);

        List<String> newColumnNames = new ArrayList<>();
        List<MigrateDataType> newColumnTypes = new ArrayList<>();

        boolean foundKeyColumn = false;
        boolean foundValueColumn = false;
        for (int i=0; i<currentColumnNames.size(); i++) {
            String columnName = currentColumnNames.get(i);
            MigrateDataType columnType = currentColumnTypes.get(i);

            if (columnName.equals(mapColumnName)) {
                // the exploded map column does not belong on Target
            }
            else if (columnName.equals(keyColumnName)) {
                foundKeyColumn = true;
                newColumnNames.add(keyColumnName);
                newColumnTypes.add(getRawMigrateDataType(Property.KEY_COLUMN_TYPE));
            } else if (columnName.equals(valueColumnName)) {
                foundValueColumn = true;
                newColumnNames.add(valueColumnName);
                newColumnTypes.add(getRawMigrateDataType(Property.VALUE_COLUMN_TYPE));
            } else {
                newColumnNames.add(columnName);
                newColumnTypes.add(columnType);
            }
        }

        if (!foundKeyColumn) {
            newColumnNames.add(keyColumnName);
            newColumnTypes.add(getRawMigrateDataType(Property.KEY_COLUMN_TYPE));
        }
        if (!foundValueColumn) {
            newColumnNames.add(valueColumnName);
            newColumnTypes.add(getRawMigrateDataType(Property.VALUE_COLUMN_TYPE));
        }

        helper.setProperty(KnownProperties.TARGET_COLUMN_NAMES, newColumnNames);
        helper.setProperty(KnownProperties.TARGET_COLUMN_TYPES, newColumnTypes);
    }

    private boolean isValidColumn(PropertyHelper helper) {
        String mapColumnName = getRawString(Property.MAP_COLUMN_NAME);
        if (null== mapColumnName || mapColumnName.isEmpty()) {
            logger.error("Value is null or empty: {}",KnownProperties.EXPLODE_MAP_ORIGIN_COLUMN_NAME);
            return false;
        }

        List<String> originNames = helper.getStringList(KnownProperties.ORIGIN_COLUMN_NAMES);
        if (!originNames.contains(mapColumnName)) {
            logger.error("Map column name {} not found on {}",mapColumnName,KnownProperties.ORIGIN_COLUMN_NAMES);
            return false;
        }
        return true;
    }

    private MigrateDataType getColumnMapDataType(PropertyHelper helper) {
        if (!isValidColumn(helper)) return null;

        String mapColumnName = getRawString(Property.MAP_COLUMN_NAME);

        List<String> originNames = helper.getStringList(KnownProperties.ORIGIN_COLUMN_NAMES);
        List<MigrateDataType> originTypes = helper.getMigrationTypeList(KnownProperties.ORIGIN_COLUMN_TYPES);

        int index = originNames.indexOf(mapColumnName);
        if (index < 0) return null;

        return originTypes.get(index);
    }

    private boolean isMapType(PropertyHelper helper) {
        MigrateDataType mdt = getColumnMapDataType(helper);
        if (mdt == null) return false;
        return mdt.getTypeClass() == Map.class;
    }

    private boolean isValid(PropertyHelper helper) {
        if (!isValidColumn(helper)) {
            logger.error("Feature requires {} be found on {}",KnownProperties.EXPLODE_MAP_ORIGIN_COLUMN_NAME, KnownProperties.ORIGIN_COLUMN_NAMES);
            return false;
        }

        if (!isMapType(helper)) {
            logger.error("Feature requires a Map type specified at {}",KnownProperties.EXPLODE_MAP_ORIGIN_COLUMN_NAME);
            return false;
        }

        return true;
    }

}