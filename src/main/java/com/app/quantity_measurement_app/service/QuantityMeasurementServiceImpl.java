package com.app.quantity_measurement_app.service;

import com.app.quantity_measurement_app.model.*;
import com.app.quantity_measurement_app.repository.QuantityMeasurementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class QuantityMeasurementServiceImpl implements IQuantityMeasurementService {

    private static final Logger logger =
            Logger.getLogger(QuantityMeasurementServiceImpl.class.getName());

    @Autowired
    private QuantityMeasurementRepository repository;

    // ================== COMPARE ==================
    @Override
    public QuantityMeasurementDTO compare(QuantityDTO thisQuantity, QuantityDTO thatQuantity) {
        validateInput(thisQuantity, thatQuantity);
        validateMeasurementTypes(thisQuantity, thatQuantity);

        IMeasurableUnit unit1 = resolveUnit(thisQuantity);
        IMeasurableUnit unit2 = resolveUnit(thatQuantity);

        boolean isEqual = compareValues(unit1, thisQuantity.getValue(), unit2, thatQuantity.getValue());
        String resultStr = isEqual ? "Equal" : "Not Equal";

        QuantityMeasurementEntity entity =
                new QuantityMeasurementEntity(thisQuantity, thatQuantity,
                        OperationType.COMPARE.name(), resultStr);

        return QuantityMeasurementDTO.from(repository.save(entity));
    }

    private <U extends IMeasurableUnit> boolean compareValues(
            U thisUnit, double thisValue, U thatUnit, double thatValue) {

        if (thisUnit instanceof TemperatureUnit) {
            double convertedThat = convertTemperatureUnit(thatUnit, thatValue, thisUnit);
            return Double.compare(round(thisValue), round(convertedThat)) == 0;
        }

        double baseVal1 = thisValue * getBaseConversionFactor(thisUnit);
        double baseVal2 = thatValue * getBaseConversionFactor(thatUnit);

        return Double.compare(round(baseVal1), round(baseVal2)) == 0;
    }

    // ================== CONVERT ==================
    @Override
    public QuantityMeasurementDTO convert(QuantityDTO thisQuantity, QuantityDTO thatQuantity) {
        validateInput(thisQuantity, thatQuantity);
        validateMeasurementTypes(thisQuantity, thatQuantity);

        IMeasurableUnit unit1 = resolveUnit(thisQuantity);
        IMeasurableUnit unit2 = resolveUnit(thatQuantity);

        double resultVal = convertTo(unit1, thisQuantity.getValue(), unit2);

        QuantityMeasurementEntity entity =
                new QuantityMeasurementEntity(thisQuantity, thatQuantity,
                        OperationType.CONVERT.name(), resultVal);

        entity.setResultUnit(thatQuantity.getUnit());
        entity.setResultMeasurementType(thatQuantity.getMeasurementType());

        return QuantityMeasurementDTO.from(repository.save(entity));
    }

    private <U extends IMeasurableUnit> double convertTo(U thisUnit, double thisValue, U targetUnit) {
        if (thisUnit instanceof TemperatureUnit) {
            return convertTemperatureUnit(thisUnit, thisValue, targetUnit);
        }

        double baseValue = thisValue * getBaseConversionFactor(thisUnit);
        return baseValue / getBaseConversionFactor(targetUnit);
    }

    // ================== TEMPERATURE ==================
    private <U extends IMeasurableUnit> double convertTemperatureUnit(
            U thisUnit, double thisValue, U targetUnit) {

        if (thisUnit == targetUnit) return thisValue;

        if (thisUnit == TemperatureUnit.FAHRENHEIT && targetUnit == TemperatureUnit.CELSIUS) {
            return (thisValue - 32) * 5 / 9;
        }
        if (thisUnit == TemperatureUnit.CELSIUS && targetUnit == TemperatureUnit.FAHRENHEIT) {
            return (thisValue * 9 / 5) + 32;
        }

        throw new UnsupportedOperationException("Unsupported temperature conversion");
    }

    // ================== ADD ==================
    @Override
    public QuantityMeasurementDTO add(QuantityDTO q1, QuantityDTO q2) {
        return add(q1, q2, q1);
    }

    @Override
    public QuantityMeasurementDTO add(QuantityDTO q1, QuantityDTO q2, QuantityDTO target) {
        return executeArithmetic(ArithmeticOperation.ADD, q1, q2, target);
    }

    // ================== SUBTRACT ==================
    @Override
    public QuantityMeasurementDTO subtract(QuantityDTO q1, QuantityDTO q2) {
        return subtract(q1, q2, q1);
    }

    @Override
    public QuantityMeasurementDTO subtract(QuantityDTO q1, QuantityDTO q2, QuantityDTO target) {
        return executeArithmetic(ArithmeticOperation.SUBTRACT, q1, q2, target);
    }

    // ================== DIVIDE ==================
    @Override
    public QuantityMeasurementDTO divide(QuantityDTO q1, QuantityDTO q2) {
        validateInput(q1, q2);
        validateMeasurementTypes(q1, q2);

        IMeasurableUnit unit2 = resolveUnit(q2);
        double base2 = q2.getValue() * getBaseConversionFactor(unit2);

        if (base2 == 0) {
            saveAndReturnError(q1, q2, OperationType.DIVIDE.name(), "Division by zero");
            throw new ArithmeticException("Division by zero");
        }

        return executeArithmetic(ArithmeticOperation.DIVIDE, q1, q2, q1);
    }

    // ================== ARITHMETIC CORE ==================
    private QuantityMeasurementDTO executeArithmetic(
            ArithmeticOperation op, QuantityDTO q1, QuantityDTO q2, QuantityDTO target) {

        validateInput(q1, q2);
        validateMeasurementTypes(q1, q2);

        IMeasurableUnit unit1 = resolveUnit(q1);
        IMeasurableUnit unit2 = resolveUnit(q2);

        validateArithmeticOperands(unit1, unit2);

        double resultValue = performArithmetic(op, unit1, q1.getValue(), unit2, q2.getValue());

        double finalValue;
        if (op == ArithmeticOperation.DIVIDE) {
            finalValue = resultValue;
            target = new QuantityDTO(finalValue, "NONE", "DIMENSIONLESS");
        } else {
            IMeasurableUnit targetU = resolveUnit(target);
            finalValue = resultValue / getBaseConversionFactor(targetU);
        }

        QuantityDTO resultDTO =
                new QuantityDTO(finalValue, target.getUnit(), target.getMeasurementType());

        QuantityMeasurementEntity entity =
                new QuantityMeasurementEntity(q1, q2, op.name(), resultDTO);

        return QuantityMeasurementDTO.from(repository.save(entity));
    }

    private enum ArithmeticOperation {
        ADD, SUBTRACT, DIVIDE
    }

    private <U extends IMeasurableUnit> void validateArithmeticOperands(U u1, U u2) {
        if (u1 instanceof TemperatureUnit || u2 instanceof TemperatureUnit) {
            throw new IllegalArgumentException("Arithmetic not supported for temperature");
        }
    }

    private <U extends IMeasurableUnit> double performArithmetic(
            ArithmeticOperation op, U u1, double v1, U u2, double v2) {

        double val1 = v1 * getBaseConversionFactor(u1);
        double val2 = v2 * getBaseConversionFactor(u2);

        switch (op) {
            case ADD: return val1 + val2;
            case SUBTRACT: return val1 - val2;
            case DIVIDE:
                if (val2 == 0) throw new ArithmeticException("Division by zero");
                return val1 / val2;
            default: throw new UnsupportedOperationException();
        }
    }

    // ================== HELPERS ==================
    private void validateInput(QuantityDTO q1, QuantityDTO q2) {
        if (q1 == null || q2 == null) {
            throw new IllegalArgumentException("Quantity cannot be null");
        }
    }

    private void validateMeasurementTypes(QuantityDTO q1, QuantityDTO q2) {
        if (!q1.getMeasurementType().equals(q2.getMeasurementType())) {
            throw new IllegalArgumentException("Invalid Measurement Type");
        }
    }

    private IMeasurableUnit resolveUnit(QuantityDTO dto) {
        try {
            switch (dto.getMeasurementType()) {
                case "LengthUnit": return LengthUnit.valueOf(dto.getUnit());
                case "VolumeUnit": return VolumeUnit.valueOf(dto.getUnit());
                case "WeightUnit": return WeightUnit.valueOf(dto.getUnit());
                case "TemperatureUnit": return TemperatureUnit.valueOf(dto.getUnit());
                default: throw new IllegalArgumentException("Invalid Measurement Type");
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid Unit");
        }
    }

    private double getBaseConversionFactor(IMeasurableUnit unit) {
        if (unit instanceof LengthUnit) {
            switch ((LengthUnit) unit) {
                case INCHES: return 1.0;
                case FEET: return 12.0;
                case YARDS: return 36.0;
                case CENTIMETERS: return 1.0 / 2.54;
            }
        } else if (unit instanceof VolumeUnit) {
            switch ((VolumeUnit) unit) {
                case LITRE: return 1.0;
                case MILLILITER: return 0.001;
                case GALLON: return 3.78541;
            }
        } else if (unit instanceof WeightUnit) {
            switch ((WeightUnit) unit) {
                case GRAM: return 1.0;
                case KILOGRAM: return 1000.0;
                case MILLIGRAM: return 0.001;
                case POUND: return 453.592;
                case TONNE: return 1000000.0;
            }
        }
        return 1.0;
    }

    private double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }

    private QuantityMeasurementDTO saveAndReturnError(
            QuantityDTO q1, QuantityDTO q2, String operation, String errorMsg) {

        logger.severe("Error: " + errorMsg);

        QuantityMeasurementEntity entity =
                new QuantityMeasurementEntity(q1, q2, operation, errorMsg, true);

        return QuantityMeasurementDTO.from(repository.save(entity));
    }

    // ================== HISTORY ==================
    @Override
    public List<QuantityMeasurementDTO> getOperationHistory(String operation) {
        return repository.findByOperation(operation)
                .stream()
                .map(QuantityMeasurementDTO::from)
                .collect(Collectors.toList());
    }

    @Override
    public List<QuantityMeasurementDTO> getMeasurementsByType(String type) {
        return repository.findByThisMeasurementType(type)
                .stream()
                .map(QuantityMeasurementDTO::from)
                .collect(Collectors.toList());
    }

    @Override
    public long getOperationCount(String operation) {
        return repository.countByOperationAndIsErrorFalse(operation);
    }

    @Override
    public List<QuantityMeasurementDTO> getErrorHistory() {
        return repository.findByIsErrorTrue()
                .stream()
                .map(QuantityMeasurementDTO::from)
                .collect(Collectors.toList());
    }
}